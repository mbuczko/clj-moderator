# Semi-automatic content moderator

Ever wanted to score incoming data based on easily defined criteria? Here it comes - content moderator, a set of basic (and some more advanced) matchers which form together a pipe where you put a data on one side and get a score on the other one.

What matchers do we have in our toolbelt?

 - ```upercase-matcher``` : returns number of capitals letters
 - ```content-size-matcher``` : returns count of all characters
 - ```bad-words-matcher``` : lowercases and splits incoming data into set of words (stripped of common non-letter characters) and compares with ```:blacklist```
 - ```bad-email-matcher``` : lowercases incoming data, wraps it into one-element set and compares with ```:blacklist```
 - ```repeats-matcher``` : returns number of subsequently repeating characters
 - ```bayes-matcher``` : returns bayes classification in binary form (0 if input was classified positively, 1 otherwise)

Naïve Bayes classification is based on [judgr](https://github.com/danielfm/judgr) and 2 wrapper function have been exposed to classify input phrase:

 - ```negative``` : trains classifier suggesting given phrase as negative
 - ```positive``` : trains classifier suggesting given phrase as positive

Note that Polish extractor is used by default.

##How matchers work

Each matcher returns either a number (integer or float) or a set (data structure). Now the magic happens - if it's a number a ```:min``` and ```:max``` parameters are checked if numeric value fits in between ```(:min <= value <= :max)```.
If this condition is met a ```:penalty``` value is added to final score. Otherwise score is not modified. Oh, by default ```:min``` is set to 0 and ```:max``` to Integer/MAX_VALUE
so there is no need to provide them both if only minimal or maximal value really matters.

In case when matcher returned a set instead of number a ```:blacklist``` set is checked if it contains at least one of elements returned by matcher. If so, again, ```:penalty``` is added to final score.

##How to define custom matcher

Well, the easiest way is to use ```defmatcher``` macro. As said before, matcher is a function which returns either a number or a set and that's the only rule a matcher has to obey:

    (defmatcher
       (fn [input]
          ...process input and return number or a set... ))

##Examples

    (require '[mbuczko.moderator :as m])

    (def blacklists {:content #{"incomplete", "bullshit"}
                     :emails #{"bad@boy.from.ru"}})

    (def candidate {:title "Amazing brand new Alfa-Romeo with A FEEEW minor glitches"
                    :contact {:phone-numbers ["1234", "55556"]}
                    :description "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Bullshit."
                    :username "bad@boy.from.ru"})

    (-> candidate
        (m/content-size-matcher :penalty 20 :field [:title] :min 71)
        (m/content-size-matcher :penalty 10 :field [:contact :phone-numbers] :max 0)
        (m/uppercase-matcher    :penalty 20 :field [:description] :min 36)
        (m/bad-words-matcher    :penalty 30 :field [:description] :blacklist (:content blacklists))
        (m/bad-email-matcher    :penalty 20 :field [:username] :blacklist (:emails blacklists))
        (m/repeats-matcher      :penalty 10 :field [:title] :min 2))

As a result a ```Candidate``` record will be returned with 3 relevant keys: ```:body``` with original data, ```:scores``` with vector of applied penalties in form of ```[penalty field matcher-name]``` and ```:final``` with sum of all applied penalties.

    {:body {:title "Amazing brand new Alfa-Romeo with A FEEEW minor glitches",
            :contact {:phone-numbers ["1234" "55556"]},
            :description "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Bullshit.",
            :username "bad@boy.from.ru"},
     :scores [[30 [:description] bad-words-matcher]
              [20 [:username] bad-email-matcher]],
              [10 [:title] repeats-matcher]],
     :final 60}

If you want to negtively classify given phrase:

    (m/negative "Ala ma kota a kot ma Alę")

Now, let's pass it through ```bayes-matcher```:

    (m/bayes-matcher {:text "Ala lubi kota"} :penalty 99 :field [:text] :min 1)

Result:

    {:body {:text "Ala lubi kota"},
     :scores [[99 [:text] bayes-matcher]],
     :final 99}

As expected, we got penalty score 99 because ```bayes-matcher``` classified our phrase as a negative one (and returned 1 in result).

##LICENSE

Copyright © Michał Buczko

Licensed under the EPL.
