(ns loggy.utils
  (:import [java.util UUID]))

(defn time-based-uuid []
  "Time sorted UUID. Algoby Nikita Prokopov(aka Tonsky)"
  (let [uuid (UUID/randomUUID)
        time (int (/ (System/currentTimeMillis) 1000))
        high (.getMostSignificantBits uuid)
        low (.getLeastSignificantBits uuid)
        new-high (bit-or (bit-and high 0x00000000FFFFFFFF)
                         (bit-shift-left time 32)) ]
    (str (UUID. new-high low))))

(defn safe-slurp [source]
  "Reads source. On fail returns nil"
  (try
    (slurp source)
    (catch Exception e
      nil)))
