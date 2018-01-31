(TeX-add-style-hook
 "planning"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-class-options
                     '(("article" "11pt")))
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("inputenc" "utf8") ("fontenc" "T1") ("ulem" "normalem")))
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "path")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "url")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "nolinkurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperbaseurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperimage")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperref")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "path")
   (TeX-run-style-hooks
    "latex2e"
    "article"
    "art11"
    "inputenc"
    "fontenc"
    "graphicx"
    "grffile"
    "longtable"
    "wrapfig"
    "rotating"
    "ulem"
    "amsmath"
    "textcomp"
    "amssymb"
    "capt-of"
    "hyperref")
   (LaTeX-add-labels
    "sec:orgee09a9e"
    "sec:org13dfd4f"
    "sec:org23b1086"
    "sec:orgc249232"
    "sec:org738ef3f"
    "sec:orgd75571b"
    "sec:orga1ee5e6"
    "sec:org2a02b37"
    "sec:orga629c32"
    "sec:org8dff2fe"
    "sec:orgf4c4cc5"
    "sec:orgff78f3e"
    "sec:org9856d5b"
    "sec:orgab6b3a5"
    "sec:org21a872e"
    "sec:org48f7c57"))
 :latex)

