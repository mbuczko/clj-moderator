(set-env!
 :source-paths   #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0-RC4" :scope "provided"]
                 [boot/core "2.5.5"]
                 [adzerk/bootlaces "0.1.13"]
                 [zilti/boot-midje "0.2.1-SNAPSHOT" :scope "test"]
                 [net.mikera/core.matrix "0.48.0"]
                 [midje "1.8.3" :scope "test"]
                 [judgr "0.3.2-SNAPSHOT"]])

(require '[adzerk.bootlaces :refer :all]
         '[zilti.boot-midje :refer [midje]])

(def +version+ "0.1.0")

(bootlaces! +version+)

(task-options!
 midje {:test-paths #{"test"}}
 pom   {:project 'mbuczko/moderator
        :version +version+
        :description "Scores incoming data based on predefined matchers"
        :url "https://github.com/mbuczko/moderator"
        :scm {:url "https://github.com/mbuczko/moderator"}
        :license {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
