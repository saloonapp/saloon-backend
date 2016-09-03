# SalooN

## TODO

### Conférence Liste

- format DateTime as ISO string...
- add lang for presentations
- s'inspirer de https://calltospeakers.com/
    - recherche : fulltext, country
    - proposer en bas de la recherche d'être notifié si qqch est ajouté et qui correspond à la recherche
- import depuis eventbrite, lanyrd, allconferencealert... pour la création d'une conf (avec talks & speakers)
- improve project quality
    - add types for Config
    - add types for external routes
    - add more types in domain (no more generic types : String, Int, Long...)
    - add & manage execution context (db, controllers, api...)
    - add tests
    - create a `/status` endpoint with version (git hash), build date & checking database
    - add API doc (https://apiblueprint.org/)
    - add key & rate limit for public API
- add pagination for conferences
- compléter l'API :
    - création / modification
    - talks / speakers
- comme sur Lanyrd, faire 3 menus :
    - conférences
    - présentations
    - speakers

### SalooN backend

- exposant vs sponsor ?
- Ajouter le 'grade' pour les sponsors (Platinum, Gold, Silver, Bronze)

## Notes

### Interesting scala libs

- [play-autosource](https://github.com/mandubian/play-autosource) : automatic REST API :)
- [doobie](https://github.com/tpolecat/doobie) : principled database access for scala
- [Précepte](https://github.com/MfgLabs/precepte) : Précepte is an opinionated scala monitoring library
- [validation](https://github.com/jto/validation) : validation api extracted from play

### Usefull tools

- [sbt-buildinfo](https://github.com/sbt/sbt-buildinfo) : add infos at build time
- [sbt-scalariform](https://github.com/sbt/sbt-scalariform) : sbt plugin adding support for source code formatting using Scalariform
- [sbt-updates](https://github.com/rtimush/sbt-updates) : SBT plugin that can check maven repositories for dependency updates
- [sbt-dependency-graph](https://github.com/jrudolph/sbt-dependency-graph) : sbt plugin to create a dependency graph for your project
- [sbt-native-packager](https://github.com/sbt/sbt-native-packager) : SBT native packager lets you build application packages in native formats

### Usefull services

- CI
    - [drone.io](https://drone.io/)
    - [Travis CI](https://travis-ci.org/)
- Code coverage
    - [](https://coveralls.io/)

### Design kits

- [Material Admin](https://wrapbootstrap.com/theme/material-admin-responsive-dark-skin-WB011H985)

### Similar websites

- http://lanyrd.com/
- http://librosweb.es/eventos/
- https://community.confoo.ca/
- https://www.papercall.io/
- http://theweeklycfp.com/
- https://www.foiresetsalons.entreprises.gouv.fr/
- http://www.parisinfo.com/ou-sortir-a-paris/foires-et-salons-a-paris
- http://www.salonsparis.cci-paris-idf.fr/index.asp?idmetapage=4&t=multi&secteur=&acces=&date=&organisateur=&parcs1=
- http://www.salons-online.com/
- http://www.gazette-salons.fr/agenda-des-salons-paris-ile-de-france
- http://www.allconferences.com/
- http://www.viparis.com/viparisFront/do/salon/paris-nord-villepinte/recherche/listecongres
- http://www.salon-entre-pros.fr/rechercher/

## Migration

### Migrate from DateTime to LocalDate for Conference : start, end & cfp.end

Run this query on Mongo :

```
db.Conferences.find({}).forEach(function(conf){
  function timestampToLocalDate(timestamp){
    var date = new Date(timestamp);
    var day = date.getDate();
    var month = date.getMonth()+1;
    var year = date.getFullYear();
    var dayStr = day < 10 ? '0'+day : day.toString();
    var monthStr = month < 10 ? '0'+month : month.toString();
    return year+'-'+monthStr+'-'+dayStr;
  }
  if(conf.start){ conf.start = timestampToLocalDate(conf.start); }
  if(conf.end){ conf.end = timestampToLocalDate(conf.end); }
  if(conf.cfp && conf.cfp.end){ conf.cfp.end = timestampToLocalDate(conf.cfp.end); }
  db.Conferences.save(conf);
})
```
