(ns clumatra.rt-test
  (:import  [java.lang.reflect Method])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure [pprint :as p]]
            [clumatra [util :as u]]
            [clumatra.test-util :refer :all])
  (:gen-class))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    ;; these are not suitable for testing
    (.getMethod clojure.lang.RT "addURL" (into-array Class [java.lang.Object]))
    (.getMethod clojure.lang.RT "baseLoader" nil)
    (.getMethod clojure.lang.RT "classForName" (into-array Class [String]))
    (.getMethod clojure.lang.RT "errPrintWriter" nil)
    (.getMethod clojure.lang.RT "getColumnNumber" (into-array Class [java.io.Reader]))
    (.getMethod clojure.lang.RT "getLineNumber" (into-array Class [java.io.Reader]))
    (.getMethod clojure.lang.RT "getLineNumberingReader" (into-array Class [java.io.Reader]))
    (.getMethod clojure.lang.RT "getResource" (into-array Class [ClassLoader String]))
    (.getMethod clojure.lang.RT "init" nil)
    (.getMethod clojure.lang.RT "isLineNumberingReader" (into-array Class [java.io.Reader]))
    (.getMethod clojure.lang.RT "lastModified" (into-array Class [java.net.URL String]))
    (.getMethod clojure.lang.RT "load" (into-array Class [String Boolean/TYPE]))
    (.getMethod clojure.lang.RT "load" (into-array Class [String]))
    (.getMethod clojure.lang.RT "loadClassForName" (into-array Class [String]))
    (.getMethod clojure.lang.RT "loadLibrary" (into-array Class [String]))
    (.getMethod clojure.lang.RT "loadResourceScript" (into-array Class [Class String Boolean/TYPE]))
    (.getMethod clojure.lang.RT "loadResourceScript" (into-array Class [Class String]))
    (.getMethod clojure.lang.RT "loadResourceScript" (into-array Class [String Boolean/TYPE]))
    (.getMethod clojure.lang.RT "loadResourceScript" (into-array Class [String]))
    (.getMethod clojure.lang.RT "makeClassLoader" nil)
    (.getMethod clojure.lang.RT "maybeLoadResourceScript" (into-array Class [String]))
    (.getMethod clojure.lang.RT "nextID" nil) ;; impure
    (.getMethod clojure.lang.RT "peekChar" (into-array Class [java.io.Reader]))
    (.getMethod clojure.lang.RT "printString" (into-array Class [Object]))
    (.getMethod clojure.lang.RT "processCommandLine" (into-array Class [(type->array-type String)]))
    (.getMethod clojure.lang.RT "readChar" (into-array Class [java.io.Reader]))
    (.getMethod clojure.lang.RT "readString" (into-array Class [String]))
    (.getMethod clojure.lang.RT "resolveClassNameInContext" (into-array Class [java.lang.String]))
    (.getMethod clojure.lang.RT "resourceAsStream" (into-array Class [ClassLoader String]))
    (.getMethod clojure.lang.RT "formatAesthetic" (into-array Class [java.io.Writer Object]))
    (.getMethod clojure.lang.RT "formatStandard" (into-array Class [java.io.Writer Object]))
    (.getMethod clojure.lang.RT "doFormat" (into-array Class [java.io.Writer String clojure.lang.ISeq]))
    (.getMethod clojure.lang.RT "var" (into-array Class [String String Object]))
    (.getMethod clojure.lang.RT "var" (into-array Class [String String]))
    (.getMethod clojure.lang.RT "print" (into-array Class [Object java.io.Writer]))

    ;; these seem to crash simulated build
    (.getMethod clojure.lang.RT "arrayToList" (into-array Class [(type->array-type Object)]))
    (.getMethod clojure.lang.RT "box" (into-array Class [Double/TYPE]))
    (.getMethod clojure.lang.RT "box" (into-array Class [Float/TYPE]))
    (.getMethod clojure.lang.RT "box" (into-array Class [Short/TYPE]))
    (.getMethod clojure.lang.RT "list" (into-array Class [Object]))
    (.getMethod clojure.lang.RT "list" (into-array Class []))
    (.getMethod clojure.lang.RT "listStar" (into-array Class [Object Object Object Object Object clojure.lang.ISeq])) [identity identity identity identity identity (fn [i] (seq [i]))]
    (.getMethod clojure.lang.RT "listStar" (into-array Class [Object Object Object Object clojure.lang.ISeq])) [identity identity identity identity (fn [i] (seq [i]))]
    (.getMethod clojure.lang.RT "listStar" (into-array Class [Object Object Object clojure.lang.ISeq])) [identity identity identity (fn [i] (seq [i]))]
    (.getMethod clojure.lang.RT "listStar" (into-array Class [Object Object clojure.lang.ISeq])) [identity identity (fn [i] (seq [i]))]
    (.getMethod clojure.lang.RT "listStar" (into-array Class [Object clojure.lang.ISeq])) [identity (fn [i] (seq [i]))]
    (.getMethod clojure.lang.RT "mapUniqueKeys" (into-array Class [(type->array-type Object)])) [(fn [i] (into-array Object [i (str i)]))]
    (.getMethod clojure.lang.RT "vector" (into-array Class [(type->array-type Object)])) [(fn [i](into-array (range i)))]
    })


(def input-fns
  {
   (.getMethod clojure.lang.RT "box" (into-array Class [Boolean/TYPE])) [even?]
   (.getMethod clojure.lang.RT "box" (into-array Class [Boolean])) [even?] ;what is this doing ?

   ;; arrays
   (.getMethod clojure.lang.RT "object_array" (into-array Class [Object])) [(fn [i] (into [] (range i)))]

   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Boolean/TYPE)])) [(fn [i](boolean-array [(even? i)]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Byte/TYPE)])) [(fn [i](byte-array [i]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Character/TYPE)])) [(fn [i](char-array [(char (+ 65 i))]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Double/TYPE)])) [(fn [i](double-array [(double i)]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Float/TYPE)])) [(fn [i](float-array [(float i)]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Integer/TYPE)])) [(fn [i](int-array [(int i)]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Long/TYPE)])) [(fn [i](long-array [i]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Object)])) [(fn [i](into-array Object [i]))]
   (.getMethod clojure.lang.RT "aclone" (into-array Class [(type->array-type Short/TYPE)])) [(fn [i](short-array [(short i)]))]

   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Boolean/TYPE)])) [(fn [i](boolean-array [(even? i)]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Byte/TYPE)])) [(fn [i](byte-array [i]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Character/TYPE)])) [(fn [i](char-array [(char (+ 65 i))]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Double/TYPE)])) [(fn [i](double-array [(double i)]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Float/TYPE)])) [(fn [i](float-array [(float i)]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Integer/TYPE)])) [(fn [i](int-array [(int i)]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Long/TYPE)])) [(fn [i](long-array [i]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Object)])) [(fn [i](into-array Object [i]))]
   (.getMethod clojure.lang.RT "alength" (into-array Class [(type->array-type Short/TYPE)])) [(fn [i](short-array [(short i)]))]

   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Boolean/TYPE) Integer/TYPE])) [(fn [i](boolean-array (inc i)))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Byte/TYPE) Integer/TYPE])) [(fn [i](byte-array (inc i)))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Character/TYPE) Integer/TYPE])) [(fn [i](char-array (inc i)))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Double/TYPE) Integer/TYPE])) [(fn [i](double-array (inc i)))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Float/TYPE) Integer/TYPE])) [(fn [i](float-array (inc i)))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Integer/TYPE) Integer/TYPE])) [(fn [i](int-array (inc i)))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Long/TYPE) Integer/TYPE])) [(fn [i](long-array (inc i)))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Object) Integer/TYPE])) [(fn [i](into-array Object (range (inc i))))]
   (.getMethod clojure.lang.RT "aget" (into-array Class [(type->array-type Short/TYPE) Integer/TYPE])) [(fn [i](short-array (inc i)))]

   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Boolean/TYPE) Integer/TYPE  Boolean/TYPE])) [(fn [i](boolean-array (inc i))) identity even?]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Byte/TYPE) Integer/TYPE Byte/TYPE])) [(fn [i](byte-array (inc i))) identity byte]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Character/TYPE) Integer/TYPE Character/TYPE])) [(fn [i](char-array (inc i))) identity (fn [i] (char (+ 65  i)))]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Double/TYPE) Integer/TYPE Double/TYPE])) [(fn [i](double-array (inc i))) identity double]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Float/TYPE) Integer/TYPE Float/TYPE])) [(fn [i](float-array (inc i))) identity float]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Integer/TYPE) Integer/TYPE Integer/TYPE])) [(fn [i](int-array (inc i))) identity int]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Long/TYPE) Integer/TYPE Long/TYPE])) [(fn [i](long-array (inc i))) identity long]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Object) Integer/TYPE Object])) [(fn [i](into-array Object (range (inc i)))) identity identity]
   (.getMethod clojure.lang.RT "aset" (into-array Class [(type->array-type Short/TYPE) Integer/TYPE Short/TYPE])) [(fn [i](short-array (inc i))) identity short]

   ;; casts
   (.getMethod clojure.lang.RT "booleanCast" (into-array Class [Boolean/TYPE])) [even?]
   (.getMethod clojure.lang.RT "booleanCast" (into-array Class [Object])) [boolean]
   (.getMethod clojure.lang.RT "byteCast" (into-array Class [Object])) [byte]
   (.getMethod clojure.lang.RT "charCast" (into-array Class [Object])) [char]
   (.getMethod clojure.lang.RT "doubleCast" (into-array Class [Object])) [double]
   (.getMethod clojure.lang.RT "floatCast" (into-array Class [Object])) [float]
   (.getMethod clojure.lang.RT "intCast" (into-array Class [Object])) [int]
   (.getMethod clojure.lang.RT "longCast" (into-array Class [Object])) [long]
   (.getMethod clojure.lang.RT "shortCast" (into-array Class [Object])) [short]
   (.getMethod clojure.lang.RT "uncheckedByteCast" (into-array Class [Object])) [byte]
   (.getMethod clojure.lang.RT "uncheckedCharCast" (into-array Class [Object])) [char]
   (.getMethod clojure.lang.RT "uncheckedDoubleCast" (into-array Class [Object])) [double]
   (.getMethod clojure.lang.RT "uncheckedFloatCast" (into-array Class [Object])) [float]
   (.getMethod clojure.lang.RT "uncheckedIntCast" (into-array Class [Object])) [int]
   (.getMethod clojure.lang.RT "uncheckedLongCast" (into-array Class [Object])) [long]
   (.getMethod clojure.lang.RT "uncheckedShortCast" (into-array Class [Object])) [short]
   
   ;; misc
   (.getMethod clojure.lang.RT "keyword" (into-array Class [String String])) [str str]
   (.getMethod clojure.lang.RT "setValues" (into-array Class [(type->array-type Object)])) [(fn [i](into-array (range i)))]
   (.getMethod clojure.lang.RT "format" (into-array Class [Object String (type->array-type Object)])) [(fn [i] nil)(fn [i] "~a")(fn [i] (into-array Object (range i)))]
   (.getMethod clojure.lang.RT "meta" (into-array Class [Object])) [(fn [i] (with-meta (vec (range i)) {:arg i}))]

   ;; seqs
   (.getMethod clojure.lang.RT "count" (into-array Class [Object])) [(fn [i] (list i))]
   (.getMethod clojure.lang.RT "length" (into-array Class [clojure.lang.ISeq])) [(fn [i] (list i))]
   (.getMethod clojure.lang.RT "toArray" (into-array Class [Object]))  [(fn [i] (list i))]
   (.getMethod clojure.lang.RT "first" (into-array Class [Object])) [(fn [i] (into [] (repeat 2 i)))]
   (.getMethod clojure.lang.RT "second" (into-array Class [Object])) [(fn [i] (into [] (repeat 3 i)))]
   (.getMethod clojure.lang.RT "third" (into-array Class [Object])) [(fn [i] (into [] (repeat 4 i)))]
   (.getMethod clojure.lang.RT "fourth" (into-array Class [Object])) [(fn [i] (into [] (repeat 5 i)))]
   (.getMethod clojure.lang.RT "nth" (into-array Class [Object Integer/TYPE])) [(fn [i] (into [] (range (inc i))))]
   (.getMethod clojure.lang.RT "nth" (into-array Class [Object Integer/TYPE Object])) [(fn [i] (into [] (range (inc i))))]
   (.getMethod clojure.lang.RT "findKey" (into-array Class [clojure.lang.Keyword clojure.lang.ISeq])) [(fn [i] (keyword (str i)))(fn [i] (list (keyword (str i)) i))]
   (.getMethod clojure.lang.RT "conj" (into-array Class [clojure.lang.IPersistentCollection Object])) [vector]
   (.getMethod clojure.lang.RT "seq" (into-array Class [Object])) [vector]
   (.getMethod clojure.lang.RT "seqOrElse" (into-array Class [Object])) [vector]
   (.getMethod clojure.lang.RT "seqToArray" (into-array Class [clojure.lang.ISeq])) [(fn [i] (seq [i]))]
   (.getMethod clojure.lang.RT "seqToPassedArray" (into-array Class [clojure.lang.ISeq (type->array-type Object)])) [(fn [i] (seq (into [] (range i)))) (fn [i] (make-array Object i))]
   (.getMethod clojure.lang.RT "seqToTypedArray" (into-array Class [Class clojure.lang.ISeq])) [type (fn [i] (seq [i]))]
   (.getMethod clojure.lang.RT "seqToTypedArray" (into-array Class [clojure.lang.ISeq])) [(fn [i] (seq [i]))]
   (.getMethod clojure.lang.RT "peek" (into-array Class [Object])) [(fn [i] (vec (range i)))]
   (.getMethod clojure.lang.RT "pop" (into-array Class [Object])) [(fn [i] (vec (range i)))]
   (.getMethod clojure.lang.RT "next" (into-array Class [Object])) [(fn [i] (seq (vec (range (inc i)))))]
   (.getMethod clojure.lang.RT "more" (into-array Class [Object])) [(fn [i] (seq (vec (range i))))]
   (.getMethod clojure.lang.RT "map" (into-array Class [(type->array-type Object)])) [(fn [i] (into-array Object (vec (range (* i 2)))))]

   ;; maps
   (.getMethod clojure.lang.RT "keys" (into-array Class [Object])) [(fn [i] {i (str i)})]
   (.getMethod clojure.lang.RT "vals" (into-array Class [Object])) [(fn [i] {i (str i)})]
   (.getMethod clojure.lang.RT "get" (into-array Class [Object Object])) [(fn [i]{i (str i)})]
   (.getMethod clojure.lang.RT "find" (into-array Class [Object Object])) [(fn [i] {i (str i)})]
   (.getMethod clojure.lang.RT "assoc" (into-array Class [Object Object Object])) [(fn [i]{i (str i)})]
   (.getMethod clojure.lang.RT "dissoc" (into-array Class [Object Object])) [(fn [i]{i (str i)})]
   ;; lists
   (.getMethod clojure.lang.RT "cons" (into-array Class [Object Object])) [identity (fn [i] (list i))]
   (.getMethod clojure.lang.RT "boundedLength" (into-array Class [clojure.lang.ISeq Integer/TYPE])) [(fn [i] (into '() (range (inc i))))]
   ;; vectors
   (.getMethod clojure.lang.RT "assocN" (into-array Class [Integer/TYPE Object Object])) [identity identity (fn [i] (into [] (range (inc i))))]
   (.getMethod clojure.lang.RT "subvec" (into-array Class [clojure.lang.IPersistentVector Integer/TYPE Integer/TYPE])) [(fn [i] (into [] (range (inc i))))]
   ;; sets
   (.getMethod clojure.lang.RT "set" (into-array Class [(type->array-type Object)])) [(fn [i](into-array (range i)))]
   (.getMethod clojure.lang.RT "contains" (into-array Class [Object Object])) [(fn [i] #{i})]

   })

(deftest-kernels (extract-methods static? clojure.lang.RT excluded-methods) input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.rt-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
