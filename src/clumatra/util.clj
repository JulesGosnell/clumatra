(ns clumatra.util
  (:import [java.lang.reflect Constructor]))

(defn ^Constructor unlock-constructor [^Class class param-types]
  (doto (.getDeclaredConstructor class param-types) (.setAccessible true)))

;; pinched from contrib
(defmacro with-ns
  "Evaluates body in another namespace.  ns is either a namespace
  object or a symbol.  This makes it possible to define functions in
  namespaces other than the current one."
  [ns & body]
  `(binding [*ns* (the-ns ~ns)]
     ~@(map (fn [form] `(eval '~form)) body)))
