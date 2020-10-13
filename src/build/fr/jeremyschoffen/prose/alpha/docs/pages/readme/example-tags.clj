(div "content")
;=> {:tag :div, :content ["content"], :type :tag}

(div {} "content")
;=> {:tag :div, :content ["content"], :type :tag}

(div (div) (div) (div))
;=> {:tag :div, :content [{:tag :div, :type :tag} {:tag :div, :type :tag} {:tag :div, :type :tag}], :type :tag}