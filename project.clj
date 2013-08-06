(defproject tailrecursion/javelin-demos "1.0.0"
  :description "Demos of the Javelin FRP library for ClojureScript."
  :url "https://github.com/tailrecursion/javelin-demos"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[tailrecursion/cljs-priority-map "1.0.0"]
                 [tailrecursion/javelin "1.0.0-SNAPSHOT"]
                 [alandipert/desiderata "1.0.1"]]
  :plugins [[lein-cljsbuild "0.3.2"]]
  :cljsbuild {:builds
              {:demos
               {:source-paths ["src/cljs"]
                :compiler {:output-to "public/demos.js"
                           :optimizations :advanced
                           :warnings true
                           ;; :optimizations :whitespace
                           ;; :pretty-print true
                           }
                :jar false}}})
