(ns mbuczko.moderator-test
  (:require [mbuczko.moderator :refer :all]
            [midje.sweet :refer :all]))

(fact "counts number of upper-case letters and adjusts score accordingly"
      (let [c {:text "Ala ma Kota a KOT ma Alę"}]
        (:final (uppercase-matcher c :penalty 10 :field [:text] :min 4)) => 10
        (:final (uppercase-matcher c :penalty 20 :field [:text] :max 5)) => 0))

(fact "counts number of elements of array or length of string and adjusts score accordingly"
      (let [c {:text "Ala ma Kota a KOT ma Alę" :array ["bad" "word"] :some {:empty []}}]
        (:final (content-size-matcher c :penalty 10 :field [:text]  :min 20)) => 10
        (:final (content-size-matcher c :penalty 20 :field [:array] :max 2))  => 20
        (:final (content-size-matcher c :penalty 30 :field [:some :empty] :min 0))  => 30))

(fact "searches for bad words and adjusts score accordingly"
      (let [c {:text "Dupa jasia mandolina"}]
        (:final (bad-words-matcher c :penalty 10 :field [:text] :blacklist #{"dupa"})) => 10
        (:final (bad-words-matcher c :penalty 20 :field [:text] :blacklist #{"jadzia"})) => 0))

(fact "searches for bad emails and adjusts score accordingly"
      (let [c {:username "Janko@Muzykant.pl"}]
        (:final (bad-email-matcher c :penalty 10 :field [:username] :blacklist #{"janko@muzykant.pl"})) => 10))

(fact "fuzzy searches for possible repeats and adjusts score accordingly"
      (let [c {:title "Super oferta"}
            d {:title "Suuuper oferta !!"}]
        (:final (repeats-matcher c :penalty 10 :field [:title] :min 2)) => 0
        (:final (repeats-matcher d :penalty 10 :field [:title] :min 2)) => 10))

(fact "bayes matcher scores correctly negatively classified phrase"
      (let [phrase (negative "Ala ma kota a kot ma Alę")]
        (:final (bayes-matcher {:text "Ala lubi kota"} :penalty 99 :field [:text] :min 1)) => 99))
