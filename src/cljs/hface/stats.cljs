(ns hface.stats
    (:require [reagent.core :as reagent]
              [hface.charts :as chart-for]
              [hface.tools :refer [every]]
              [cognitect.transit :as t]
              [goog.net.XhrIo :as xhr]))

(defonce refresh-interval 2000)
;; (def stats (atom {}))
(def stats (reagent/atom {}))

(defn refresh-stats [stats]
  (xhr/send "cluster-stats"
            (fn [event]
              (let [t-reader (t/reader :json)
                    res (-> event .-target .getResponseText)]
                (reset! stats (t/read t-reader res))))
            "GET"))

(defn refresh-cpu [stats chart]
  (when chart
    (let [cpu-usage (-> @stats :aggregated 
                               :top 
                               :os-process-cpu-load)]
      (.load chart (clj->js {:columns [["cpu usage" cpu-usage]]})))))

(defn mem-used [{:keys [memory-heap-memory-used 
                        memory-non-heap-memory-used 
                        os-memory-total-physical-memory]}]
  (-> (+ memory-heap-memory-used memory-non-heap-memory-used)
      (* 100) ;; i.e. 100%
      (/ os-memory-total-physical-memory)))

(defn refresh-mem [stats chart]
  (when chart
    (let [mem-usage (-> @stats :aggregated 
                               :top
                               mem-used)]
      (.load chart (clj->js {:columns [["memory usage" mem-usage]]})))))

(def s (atom -1)) ;; TODO: refactor to use real timeseries seconds vs. a dummy global sequence

(defn update-map-area [m-name stats chart]
  (when chart
    (let [m-stats (-> @stats :aggregated 
                             :map-stats
                             m-name)]
                             ;; (.get (keyword m-name)))]
      
      (.flow chart (clj->js {:columns [;;["x" (.getSeconds (js/Date.))]
                                       ["x" (swap! s #(+ % 2))]
                                       ["puts" (:put-rate m-stats)]
                                       ["hits" (:hit-rate m-stats)]
                                       ["gets" (:get-rate m-stats)]]
                             :duration 1500})))))

(defn cpu-usage [clazz]
  (let [cpu-div (reagent/atom {})]
    (every refresh-interval #(refresh-cpu stats @cpu-div))
    (js/setTimeout #(reset! cpu-div (chart-for/cpu-gauge clazz)) 100)
    (fn []
      [:div {:class clazz}])))

(defn memory-usage [clazz]
  (let [mem-div (reagent/atom {})]
    (every refresh-interval #(refresh-mem stats @mem-div))
    (js/setTimeout #(reset! mem-div (chart-for/mem-gauge clazz)) 100)
    (fn []
      [:div {:class clazz}])))

(defn map-stats [m-name]
  (let [map-stats-div (reagent/atom {})]
    (every refresh-interval #(update-map-area m-name stats @map-stats-div))
    (js/setTimeout #(reset! map-stats-div (chart-for/map-area-chart)) 100)
    (fn []
      [:div.map-stats])))

(defn members [s]
  (map name (-> s :per-node keys)))

(defn cluster-members []
  [:ul.nav.nav-second-level
   (for [member (members @stats)]
     ^{:key member} [:li [:a {:href "#"} member]])])

(every refresh-interval #(refresh-stats stats))



;; raw play

(defn show-map-stats [s]
  (for [[k v] (-> s :aggregated :map-stats)] 
    [:div (str k) "-> { get-count: " (:get-count v) 
                     ", put-count: " (:put-count v) 
                     ", get-rate: " (:get-rate v) 
                     ", put-rate: " (:put-rate v) 
                     ", hit-rate: " (:hit-rate v) 
     "}"]))

(defn show-stats [stats]
  [:div
    [:div [:h6 "members: " (for [m (members @stats)] [:span m " "])]]
    ;; [:div.cpu-usage]
    [:div.map-stats]
    [:div "maps: " (show-map-stats @stats)]])