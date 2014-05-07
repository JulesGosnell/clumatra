(ns clumatra.rt-test
  (:import  [java.lang.reflect Method]
            [java.io Reader Writer]
            [clojure.lang RT Keyword ISeq IPersistentVector IPersistentCollection])
  (:require [clojure.test :refer :all]
            [clojure.core
             [reducers :as r]
             [rrb-vector :as v]]
            [clojure [pprint :as p]]
            [clumatra.util :refer :all]
            [clumatra.test-util :refer :all])
  (:gen-class))

;;------------------------------------------------------------------------------

(def excluded-methods
  #{
    ;; these are not suitable for testing
    (fetch-method RT "addURL"                    [Object])
    (fetch-method RT "baseLoader"                [])
    (fetch-method RT "classForName"              [String])
    (fetch-method RT "errPrintWriter"            [])
    (fetch-method RT "getColumnNumber"           [Reader])
    (fetch-method RT "getLineNumber"             [Reader])
    (fetch-method RT "getLineNumberingReader"    [Reader])
    (fetch-method RT "getResource"               [ClassLoader String])
    (fetch-method RT "init"                      [])
    (fetch-method RT "isLineNumberingReader"     [Reader])
    (fetch-method RT "lastModified"              [java.net.URL String])
    (fetch-method RT "load"                      [String Boolean/TYPE])
    (fetch-method RT "load"                      [String])
    (fetch-method RT "loadClassForName"          [String])
    (fetch-method RT "loadLibrary"               [String])
    (fetch-method RT "loadResourceScript"        [Class String Boolean/TYPE])
    (fetch-method RT "loadResourceScript"        [Class String])
    (fetch-method RT "loadResourceScript"        [String Boolean/TYPE])
    (fetch-method RT "loadResourceScript"        [String])
    (fetch-method RT "makeClassLoader"           [])
    (fetch-method RT "maybeLoadResourceScript"   [String])
    (fetch-method RT "nextID"                    []) ;; impure
    (fetch-method RT "peekChar"                  [Reader])
    (fetch-method RT "printString"               [Object])
    (fetch-method RT "processCommandLine"        [(type->array-type String)])
    (fetch-method RT "readChar"                  [Reader])
    (fetch-method RT "readString"                [String])
    (fetch-method RT "resolveClassNameInContext" [String])
    (fetch-method RT "resourceAsStream"          [ClassLoader String])
    (fetch-method RT "formatAesthetic"           [Writer Object])
    (fetch-method RT "formatStandard"            [Writer Object])
    (fetch-method RT "doFormat"                  [Writer String ISeq])
    (fetch-method RT "var"                       [String String Object])
    (fetch-method RT "var"                       [String String])
    (fetch-method RT "print"                     [Object Writer])

    ;; these seem to crash simulated build
    (fetch-method RT "arrayToList"   [(type->array-type Object)])
    (fetch-method RT "box"           [Double/TYPE])
    (fetch-method RT "box"           [Float/TYPE])
    (fetch-method RT "box"           [Short/TYPE])
    (fetch-method RT "list"          [Object])
    (fetch-method RT "list"          [])
    (fetch-method RT "listStar"      [Object Object Object Object Object ISeq]) [identity identity identity identity identity (fn [i] (seq [i]))]
    (fetch-method RT "listStar"      [Object Object Object Object ISeq]) [identity identity identity identity (fn [i] (seq [i]))]
    (fetch-method RT "listStar"      [Object Object Object ISeq]) [identity identity identity (fn [i] (seq [i]))]
    (fetch-method RT "listStar"      [Object Object ISeq]) [identity identity (fn [i] (seq [i]))]
    (fetch-method RT "listStar"      [Object ISeq]) [identity (fn [i] (seq [i]))]
    (fetch-method RT "mapUniqueKeys" [(type->array-type Object)]) [(fn [i] (into-array Object [i (str i)]))]
    (fetch-method RT "vector"        [(type->array-type Object)]) [(fn [i](into-array (range i)))]
    })


(def input-fns
  {
   (fetch-method RT "box" [Boolean/TYPE]) [even?]
   (fetch-method RT "box" [Boolean]) [even?] ;what is this doing ?

   ;; arrays
   (fetch-method RT "object_array" [Object]) [(fn [i] (into [] (range i)))]

   (fetch-method RT "aclone" [(type->array-type Boolean/TYPE)]) [(fn [i](boolean-array [(even? i)]))]
   (fetch-method RT "aclone" [(type->array-type Byte/TYPE)]) [(fn [i](byte-array [i]))]
   (fetch-method RT "aclone" [(type->array-type Character/TYPE)]) [(fn [i](char-array [(char (+ 65 i))]))]
   (fetch-method RT "aclone" [(type->array-type Double/TYPE)]) [(fn [i](double-array [(double i)]))]
   (fetch-method RT "aclone" [(type->array-type Float/TYPE)]) [(fn [i](float-array [(float i)]))]
   (fetch-method RT "aclone" [(type->array-type Integer/TYPE)]) [(fn [i](int-array [(int i)]))]
   (fetch-method RT "aclone" [(type->array-type Long/TYPE)]) [(fn [i](long-array [i]))]
   (fetch-method RT "aclone" [(type->array-type Object)]) [(fn [i](into-array Object [i]))]
   (fetch-method RT "aclone" [(type->array-type Short/TYPE)]) [(fn [i](short-array [(short i)]))]

   (fetch-method RT "alength" [(type->array-type Boolean/TYPE)]) [(fn [i](boolean-array [(even? i)]))]
   (fetch-method RT "alength" [(type->array-type Byte/TYPE)]) [(fn [i](byte-array [i]))]
   (fetch-method RT "alength" [(type->array-type Character/TYPE)]) [(fn [i](char-array [(char (+ 65 i))]))]
   (fetch-method RT "alength" [(type->array-type Double/TYPE)]) [(fn [i](double-array [(double i)]))]
   (fetch-method RT "alength" [(type->array-type Float/TYPE)]) [(fn [i](float-array [(float i)]))]
   (fetch-method RT "alength" [(type->array-type Integer/TYPE)]) [(fn [i](int-array [(int i)]))]
   (fetch-method RT "alength" [(type->array-type Long/TYPE)]) [(fn [i](long-array [i]))]
   (fetch-method RT "alength" [(type->array-type Object)]) [(fn [i](into-array Object [i]))]
   (fetch-method RT "alength" [(type->array-type Short/TYPE)]) [(fn [i](short-array [(short i)]))]

   (fetch-method RT "aget" [(type->array-type Boolean/TYPE) Integer/TYPE]) [(fn [i](boolean-array (inc i)))]
   (fetch-method RT "aget" [(type->array-type Byte/TYPE) Integer/TYPE]) [(fn [i](byte-array (inc i)))]
   (fetch-method RT "aget" [(type->array-type Character/TYPE) Integer/TYPE]) [(fn [i](char-array (inc i)))]
   (fetch-method RT "aget" [(type->array-type Double/TYPE) Integer/TYPE]) [(fn [i](double-array (inc i)))]
   (fetch-method RT "aget" [(type->array-type Float/TYPE) Integer/TYPE]) [(fn [i](float-array (inc i)))]
   (fetch-method RT "aget" [(type->array-type Integer/TYPE) Integer/TYPE]) [(fn [i](int-array (inc i)))]
   (fetch-method RT "aget" [(type->array-type Long/TYPE) Integer/TYPE]) [(fn [i](long-array (inc i)))]
   (fetch-method RT "aget" [(type->array-type Object) Integer/TYPE]) [(fn [i](into-array Object (range (inc i))))]
   (fetch-method RT "aget" [(type->array-type Short/TYPE) Integer/TYPE]) [(fn [i](short-array (inc i)))]

   (fetch-method RT "aset" [(type->array-type Boolean/TYPE) Integer/TYPE  Boolean/TYPE]) [(fn [i](boolean-array (inc i))) identity even?]
   (fetch-method RT "aset" [(type->array-type Byte/TYPE) Integer/TYPE Byte/TYPE]) [(fn [i](byte-array (inc i))) identity byte]
   (fetch-method RT "aset" [(type->array-type Character/TYPE) Integer/TYPE Character/TYPE]) [(fn [i](char-array (inc i))) identity (fn [i] (char (+ 65  i)))]
   (fetch-method RT "aset" [(type->array-type Double/TYPE) Integer/TYPE Double/TYPE]) [(fn [i](double-array (inc i))) identity double]
   (fetch-method RT "aset" [(type->array-type Float/TYPE) Integer/TYPE Float/TYPE]) [(fn [i](float-array (inc i))) identity float]
   (fetch-method RT "aset" [(type->array-type Integer/TYPE) Integer/TYPE Integer/TYPE]) [(fn [i](int-array (inc i))) identity int]
   (fetch-method RT "aset" [(type->array-type Long/TYPE) Integer/TYPE Long/TYPE]) [(fn [i](long-array (inc i))) identity long]
   (fetch-method RT "aset" [(type->array-type Object) Integer/TYPE Object]) [(fn [i](into-array Object (range (inc i)))) identity identity]
   (fetch-method RT "aset" [(type->array-type Short/TYPE) Integer/TYPE Short/TYPE]) [(fn [i](short-array (inc i))) identity short]

   ;; casts
   (fetch-method RT "booleanCast" [Boolean/TYPE]) [even?]
   (fetch-method RT "booleanCast" [Object]) [boolean]
   (fetch-method RT "byteCast" [Object]) [byte]
   (fetch-method RT "charCast" [Object]) [char]
   (fetch-method RT "doubleCast" [Object]) [double]
   (fetch-method RT "floatCast" [Object]) [float]
   (fetch-method RT "intCast" [Object]) [int]
   (fetch-method RT "longCast" [Object]) [long]
   (fetch-method RT "shortCast" [Object]) [short]
   (fetch-method RT "uncheckedByteCast" [Object]) [byte]
   (fetch-method RT "uncheckedCharCast" [Object]) [char]
   (fetch-method RT "uncheckedDoubleCast" [Object]) [double]
   (fetch-method RT "uncheckedFloatCast" [Object]) [float]
   (fetch-method RT "uncheckedIntCast" [Object]) [int]
   (fetch-method RT "uncheckedLongCast" [Object]) [long]
   (fetch-method RT "uncheckedShortCast" [Object]) [short]
   
   ;; misc
   (fetch-method RT "keyword" [String String]) [str str]
   (fetch-method RT "setValues" [(type->array-type Object)]) [(fn [i](into-array (range i)))]
   (fetch-method RT "format" [Object String (type->array-type Object)]) [(fn [i] nil)(fn [i] "~a")(fn [i] (into-array Object (range i)))]
   (fetch-method RT "meta" [Object]) [(fn [i] (with-meta (vec (range i)) {:arg i}))]

   ;; seqs
   (fetch-method RT "count" [Object]) [(fn [i] (list i))]
   (fetch-method RT "length" [ISeq]) [(fn [i] (list i))]
   (fetch-method RT "toArray" [Object])  [(fn [i] (list i))]
   (fetch-method RT "first" [Object]) [(fn [i] (into [] (repeat 2 i)))]
   (fetch-method RT "second" [Object]) [(fn [i] (into [] (repeat 3 i)))]
   (fetch-method RT "third" [Object]) [(fn [i] (into [] (repeat 4 i)))]
   (fetch-method RT "fourth" [Object]) [(fn [i] (into [] (repeat 5 i)))]
   (fetch-method RT "nth" [Object Integer/TYPE]) [(fn [i] (into [] (range (inc i))))]
   (fetch-method RT "nth" [Object Integer/TYPE Object]) [(fn [i] (into [] (range (inc i))))]
   (fetch-method RT "findKey" [Keyword ISeq]) [(fn [i] (keyword (str i)))(fn [i] (list (keyword (str i)) i))]
   (fetch-method RT "conj" [IPersistentCollection Object]) [vector]
   (fetch-method RT "seq" [Object]) [vector]
   (fetch-method RT "seqOrElse" [Object]) [vector]
   (fetch-method RT "seqToArray" [ISeq]) [(fn [i] (seq [i]))]
   (fetch-method RT "seqToPassedArray" [ISeq (type->array-type Object)]) [(fn [i] (seq (into [] (range i)))) (fn [i] (make-array Object i))]
   (fetch-method RT "seqToTypedArray" [Class ISeq]) [type (fn [i] (seq [i]))]
   (fetch-method RT "seqToTypedArray" [ISeq]) [(fn [i] (seq [i]))]
   (fetch-method RT "peek" [Object]) [(fn [i] (vec (range i)))]
   (fetch-method RT "pop" [Object]) [(fn [i] (vec (range i)))]
   (fetch-method RT "next" [Object]) [(fn [i] (seq (vec (range (inc i)))))]
   (fetch-method RT "more" [Object]) [(fn [i] (seq (vec (range i))))]
   (fetch-method RT "map" [(type->array-type Object)]) [(fn [i] (into-array Object (vec (range (* i 2)))))]

   ;; maps
   (fetch-method RT "keys" [Object]) [(fn [i] {i (str i)})]
   (fetch-method RT "vals" [Object]) [(fn [i] {i (str i)})]
   (fetch-method RT "get" [Object Object]) [(fn [i]{i (str i)})]
   (fetch-method RT "find" [Object Object]) [(fn [i] {i (str i)})]
   (fetch-method RT "assoc" [Object Object Object]) [(fn [i]{i (str i)})]
   (fetch-method RT "dissoc" [Object Object]) [(fn [i]{i (str i)})]
   ;; lists
   (fetch-method RT "cons" [Object Object]) [identity (fn [i] (list i))]
   (fetch-method RT "boundedLength" [ISeq Integer/TYPE]) [(fn [i] (into '() (range (inc i))))]
   ;; vectors
   (fetch-method RT "assocN" [Integer/TYPE Object Object]) [identity identity (fn [i] (into [] (range (inc i))))]
   (fetch-method RT "subvec" [IPersistentVector Integer/TYPE Integer/TYPE]) [(fn [i] (into [] (range (inc i))))]
   ;; sets
   (fetch-method RT "set" [(type->array-type Object)]) [(fn [i](into-array (range i)))]
   (fetch-method RT "contains" [Object Object]) [(fn [i] #{i})]

   })

(deftest-kernels (extract-methods static? RT excluded-methods) inc input-fns)

;;------------------------------------------------------------------------------

(defn -main
  "run individual tests by name from the command line...no args runs all the tests."
  [& args]
  (let [interns (ns-interns 'clumatra.rt-test)]
    (if args
      (doseq [test args]
        (test-vars [(interns (symbol test))]))
      (test-vars (vals interns)))))
