(ns clumatra.util
  (:import [java.lang.reflect Constructor Method]))

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

;; pinched from core reducers...
(defmacro compile-if [exp then else]
  (if (try (eval exp) (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

(defn warn-if-nil [value message]
  (if (nil? value)
    (do
      (println message)
      value)
    value))

(defn fetch-method
  ([^Class class name]
     (warn-if-nil
      (first (filter (fn [^Method m] (= (.getName m) name)) (.getMethods class)))
      (str "ERROR: NO SUCH METHOD: " class "." name)))
  ([^Class class name parameter-types]
     (.getMethod class name (into-array Class parameter-types)))
  ([^Class class name ^Class return-type parameter-types]
     (warn-if-nil
      (first
       (filter
        (fn [^Method m]
          (and (= (.getName m) name)
               (= (.getReturnType m) return-type)
               (= (seq (.getParameterTypes m)) (seq parameter-types))))
        (.getMethods class)))
      (str "ERROR: NO SUCH METHOD: "  return-type " " class "." name parameter-types))))
    

