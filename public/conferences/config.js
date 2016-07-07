var Config = (function(){
    var ConferenceForm = {
        $elt: $('form.conference-form'),
        $id: $('input#id'),
        $siteUrl: $('input#siteUrl'),
        $name: $('input#name'),
        $description: $('textarea#description'),
        $dates: $('input#dates'),
        $tags: $('select#tags'),
        $twitterAccount: $('input#social_twitter_account'),
        $logo: $('input#logo'),
        $createdBy_name: $('input#createdBy_name'),
        $createdBy_email: $('input#createdBy_email'),
        $createdBy_siteUrl: $('input#createdBy_siteUrl'),
        $createdBy_twitter: $('input#createdBy_twitter'),
        $createdBy_public: $('input#createdBy_public')
    };
    var Url = {
        metaScraper: function(url){ return '/tools/scrapers/utils/metas?url='+url; },
        twitterScraper: function(account){ return '/tools/scrapers/twitter/profil?account='+account; },
        search: function(opts){
            var params = [];
            for(var i in opts){
                params.push(i+'='+opts[i]);
            }
            return '/api/conferences/search?'+params.join('&');
        }
    };
    var Api = {
        getMetas: function(url){
            return $.get(Url.metaScraper(url));
        },
        getTwitterAccount: function(account){
            return $.get(Url.twitterScraper(account)).then(function(data){
                return data ? data.result : undefined;
            });
        },
        getDuplicatedConferences: function(){
            var siteUrl = ConferenceForm.$siteUrl.val();
            var dates = ConferenceForm.$dates.val();
            return $.when(
                siteUrl ? $.get(Url.search({q: siteUrl})) : [{result: []}],
                dates ? $.get(Url.search({period: dates})) : [{result: []}]
            ).then(function(r1, r2){
                return Utils.unique(r1[0].result.concat(r2[0].result), function(c){ return c.id; });
            });
        }
    };
    return {
        Url: Url,
        Api: Api,
        Form: {
            Conference: ConferenceForm
        }
    };
})();
