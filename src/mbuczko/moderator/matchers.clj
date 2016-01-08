(ns mbuczko.moderator.matchers
  (:require [clojure.string             :as str]
            [judgr.core                 :as jc]
            [judgr.settings             :as js]
            [mbuczko.moderator.distance :as d]))

(defrecord Candidate [body scores final])

(def ^:dynamic *rule-name* nil)

(defn- wrap-candidate [body]
  (if (instance? Candidate body) body (map->Candidate {:body body :scores [] :final 0})))

(defn- update-scores [candidate field penalty]
  (update-in candidate [:scores] conj [penalty field *rule-name*]))

(defn- update-final [candidate field penalty]
  (update-in candidate [:final] + penalty))

(defn update-candidate [candidate field penalty]
  (-> candidate
      (update-final  field penalty)
      (update-scores field penalty)))

(defn in-range? [value min max candidate field penalty]
  (if (and (>= value min) (<= value max))
    (update-candidate candidate field penalty)
    candidate))

(defn in-blacklist? [words blacklist candidate field penalty]
  (if (some blacklist words)
    (update-candidate candidate field penalty)
    candidate))

(def bayes-settings
  (js/update-settings js/settings [:extractor :type] :polish-text))

(def classifier
  (jc/classifier-from bayes-settings))

(defn negative [phrase]
  (.train! classifier phrase :negative))

(defn positive [phrase]
  (.train! classifier phrase :positive))

(defn classify [phrase]
  (.classify classifier phrase))

(defn- do-match
  "Performs actual matching by invoking matcher-fn against its input's :body.
  Returned result is passed either to in-blacklist? or in-range? functions
  along with matcher options (penalty, min-max range or blacklist to scan)."
  [matcher-fn matcher-def {:keys [penalty field blacklist min max]
                           :or   {penalty 0 min 0 max Integer/MAX_VALUE}}]
  (let [candidate (wrap-candidate matcher-def)
        result (matcher-fn (get-in (:body candidate) field))]
    (if (set? result)
      (in-blacklist? result blacklist candidate field penalty)
      (in-range? result min max candidate field penalty))))

(defmacro defmatcher [name matcher-fn]
  `(defn ~name ~'[body & options]
     (binding [*rule-name* (:name (meta (var ~name)))]
       (do-match ~matcher-fn ~'body ~'options))))

(defmatcher uppercase-matcher
  (fn [input]
    (.length (str/replace input #"[^A-ZĄĘŚŻŹĆŃÓŁ]" ""))))

(defmatcher content-size-matcher
  (fn [input]
    (count input)))

(defmatcher bad-words-matcher
  (fn [input]
    (set (-> input
             (str/replace #"[!,\.\-\/=\~\(\)\*&#@]" "")
             (str/lower-case)
             (str/split #"\s")))))

(defmatcher bad-email-matcher
  (fn [input]
    (conj #{} (str/lower-case input))))

(defmatcher repeats-matcher
  (fn [input]
    (d/da-lev input (apply str (map first (partition-by identity input))))))

(defmatcher bayes-matcher
  (fn [input]
    (if (= (classify input) :negative) 1 0)))
