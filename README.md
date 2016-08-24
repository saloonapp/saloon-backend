# SalooN

## deploy

Clean : https://devcenter.heroku.com/articles/scala-support#clean-builds

## Installation

### Linux

### OS X

- Install scala : `brew install scala`
- Install sbt : `brew install sbt`
- Download PlayFramework : `wget http://downloads.typesafe.com/typesafe-activator/1.3.2/typesafe-activator-1.3.2.zip`
- Add activator to your $PATH
- Install & start mongodb
- Run the project : `cd /path/to/saloon/backend/ && activator run`

## TODO

- Conférence Liste
    - faire une liste de speakers & faire un auto-complete pour les talks
    - add pagination for conferences
    - pour la création d'une conf, proposer l'import depuis eventbrite, lanyrd...
    - améliorer l'API :
        - création / modification
        - talks / speakers
    - comme sur Lanyrd, faire 3 menus :
        - conférences
        - présentations
        - speakers
- SalooN
    - exposant vs sponsor ?
    - Ajouter le 'grade' pour les sponsors (Platinum, Gold, Silver, Bronze)

## Technical

- [play-autosource](https://github.com/mandubian/play-autosource) : automatic REST API :)
- [Timepicker](https://eonasdan.github.io/bootstrap-datetimepicker/)

## Design

- [Material Admin](http://192.185.228.226/projects/ma/v1-4-1/jQuery/index.html)
- [Materialism](http://www.theme-guys.com/materialism/html/index.html)

## Sites événements

Avec contact orga :

- DONE (tel only) https://www.foiresetsalons.entreprises.gouv.fr/
- (email & tel) http://www.parisinfo.com/ou-sortir-a-paris/foires-et-salons-a-paris
- (email & tel) http://www.salonsparis.cci-paris-idf.fr/index.asp?idmetapage=4&t=multi&secteur=&acces=&date=&organisateur=&parcs1=
- (via formulaire) http://www.salons-online.com/
- (via formulaire) http://www.gazette-salons.fr/agenda-des-salons-paris-ile-de-france
- (login nécessaire) http://www.allconferences.com/

Sans contact :

- http://www.viparis.com/viparisFront/do/salon/paris-nord-villepinte/recherche/listecongres
- http://www.salon-entre-pros.fr/rechercher/
