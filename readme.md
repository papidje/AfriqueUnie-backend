# Todo : 
* Ajouter la gestion des exception 
* Configurer les messages d'erreurs
* Ajouter le roolback aux scripts sql
* Supprimer les user non validés / non affectés à une école

# Fonctionnalités
## Gerer avec les bon roles 

* Crud Ecole 
* Crud Users 
* Crud eleves 
* Crud Paiements

# Etapes

## For All User
* Create account
* Validate account

## For Super Admin
* Create School
* Load schools
* Load School users (Users without school)
* Give users Admin role on School

## For Admin
* login
* Load School data
* Create School year
* Create School classes
* Load School users (Users without school)
* Give users roles

## For Other Users (Accounter)
* Login
* Load school data
* Load school classes
* Create Fee by class
* Create Students
* Create student enrolment/Add student to a class
* Create payment ()
