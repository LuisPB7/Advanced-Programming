; -------------------------- GROUP 3 --------------------------- ;

; ----------------------- Global variables --------------------- ;
(defvar *class-inheritance-lists* (make-hash-table)) ; each class is mapped to a list of its superclasses
(defvar *class-attributes-lists* (make-hash-table)) ; each class is mapped to a list of its attributes (inherited as well)

; ----------------------- Helper functions --------------------- ;
	
(defun create-precedence-list (inheritance-list) ; function that returns the class' superclasses
	(let* ((superclasses (cdr inheritance-list)) (all-superclasses superclasses))
		(dolist (superclass superclasses) 
			(setf all-superclasses (append all-superclasses (gethash superclass *class-inheritance-lists*))))
		all-superclasses))
		
(defun create-attributes-list (inheritance slots) ; function that returns the class' attributes, inherited as well
	(let ( (allSlots slots) )
		(if (listp inheritance)
			(dolist (superclass (cdr inheritance)) 
				(setf allSlots (append allSlots (gethash superclass *class-attributes-lists*)))))
		allSlots))
		
(defun create-object (className values-vector) ; function that returns the hash table used to represent the object
	(let ( (attributes (gethash className *class-attributes-lists*)) (object (make-hash-table)))
		(loop for i from 0 to (1- (length attributes)) do (progn
			(setf (gethash (nth i attributes) object) (aref values-vector i))))
		object))

; -------------------------------------------------------- ;		
; ---------------------- MAIN MACRO ---------------------- ;
; -------------------------------------------------------- ;
			                          
(defmacro def-class (inheritance &rest slots)
    (let* ((className) (attr-list))
    (if (listp inheritance) (progn ;if
		(setf className  (nth 0 inheritance))
		(setf (gethash className *class-inheritance-lists*) (create-precedence-list inheritance))) ;fill the class' precedence list
		(setf className inheritance)) ;else
	(setf (gethash className *class-attributes-lists*) (create-attributes-list inheritance slots)) ;fill the class' attributes, inherited as well
	(setf attr-list (gethash className *class-attributes-lists*))	
  `(progn
		
        (defun ,(intern (format nil "MAKE-~a" className)) (&key ,@attr-list) ;Constructor
            (list ',className (create-object ',className (vector ,@attr-list))))
            
        (defun ,(intern (format nil "~a?" className)) (,className) ;Recognizer
			(if (or (equalp ',className (nth 0 ,className)) (gethash (nth 0 ,className) *class-inheritance-lists*)) ;it might be a direct instance or inherited
				T 
				NIL))
        
        ,@(loop for i from 0 to (1- (length (gethash className *class-attributes-lists*))) collect ;Getters
			`(defun ,(intern (format nil "~a-~a" className (nth i (gethash className *class-attributes-lists*)))) (,className) ;create a getter for every attribute, inherited as well
				(if (,(intern (format nil "~a?" className)) ,className)
					(gethash ',(nth i (gethash className *class-attributes-lists*)) (nth 1 ,className)) 'ERROR  )))
		
		,@(loop for i from 0 to (1- (length (gethash className *class-attributes-lists*))) collect ;Setters
			`(defun ,(intern (format nil "~a-SET-~a!" className (nth i (gethash className *class-attributes-lists*)))) (,className value) ;create a setter for every attribute, inherited as well
				(if (,(intern (format nil "~a?" className)) ,className)
					(setf (gethash ',(nth i (gethash className *class-attributes-lists*)) (nth 1 ,className)) value)))))))                         
