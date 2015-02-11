(ns hface.c3
  (:require [hface.tools :refer [to-css-class]]))

(defn gauge [elem {:keys [data-is init-value height]
                   :or   {data-is "usage"
                          init-value 0
                          height 180}}]
  (.generate js/c3 (clj->js 
                     {:bindto (to-css-class elem)
                      :data 
                      {:columns [[data-is init-value]]
                       :type "gauge"}
                      :gauge {}
                      :color 
                      {:pattern ["#60B044" "#F6C600" "#F97600" "#FF0000"]
                       :threshold {:values [30 60 90 100]}}
                      :size {:height height}})))

(defn map-area [elem & {:keys [x-span x-label x-format]
                        :or   {x-span (range 1 20)
                               x-label "seconds"
                               x-format "%S"}}]
  (let [zeros (replicate (count x-span) 0)]
    (.log js/console (clj->js (cons "x" x-span)))
    (.generate js/c3 (clj->js 
                       {:bindto (to-css-class elem)
                        :data 
                          {:x "x"
                           :columns [(cons "x" x-span)
                                     (cons "puts" zeros)
                                     (cons "hits" zeros)
                                     (cons "gets" zeros)]
                           :types {:gets "area-spline"
                                   :puts "area-spline"
                                   :hits "area-spline"}
                           :names {:gets "gets/s"
                                   :puts "puts/s"
                                   :hits "total hits/s"}}
                        :axis {:y "ops / s"
                               :x 
                                 {:label x-label
                                  ;; :type "timeseries"
                                  ;; :tick {:format x-format}
                                  }}}))))