/*
    Scripts specifics for conference form
 */

// automatically fill conference form with website metas
(function(){
    var form = Config.Form.Conference;
    if(form.exists()) {
        form.$siteUrl.on('change', function () {
            var url = $(this).val();
            if (Utils.isUrl(url)) {
                Config.Api.getMetas(url).then(function (metas) {
                    formValue(form.$name, Utils.getSafe(metas, 'title.0'));
                    formValue(form.$description, Utils.getSafe(metas, 'description.0'));
                    formValue(form.$twitterAccount, (Utils.getSafe(metas, 'all.twitter:site.0') || '').replace('@', ''));
                    setTags(metas, 'keywords', form.$tags);
                });
            }
        });
    }
    function formValue(elt, value){
        if(elt.val() === '' && value){
            elt.val(value).change();
        }
    }
    function setTags(data, dataSelector, elt){
        if(elt.val() === null){
            var tags = Utils.getSafe(data, dataSelector) || [];
            tags.map(function(tag){
                if(elt.find('option[value="'+tag+'"]').length === 0){
                    elt.append('<option value="'+tag+'">'+tag+'</option>');
                }
            });
            elt.select2('val', tags);
        }
    }
})();

// get twitter avatar to fill logo
(function(){
    var form = Config.Form.Conference;
    if(form.exists()) {
        form.$twitterAccount.on('change', function () {
            if (form.$logo.val() === '') {
                Config.Api.getTwitterAccount($(this).val()).then(function (account) {
                    if (account && form.$logo.val() === '') {
                        form.$logo.val(account.avatar).change();
                    }
                });
            }
        });
    }
})();

// save and fill conference user data
(function(){
    var storageKey = 'conference-createdBy';
    var form = Config.Form.User;
    if(form.exists()){
        setFormUser(Storage.get(storageKey));
        form.$elt.on('submit', function(){
            Storage.set(storageKey, getFormUser());
        });
    }
    function getFormUser(){
        return {
            name: form.$createdBy_name.val(),
            email: form.$createdBy_email.val(),
            siteUrl: form.$createdBy_siteUrl.val(),
            twitter: form.$createdBy_twitter.val(),
            public: form.$createdBy_public.prop('checked')
        };
    }
    function setFormUser(user){
        function setInput(elt, value){ if(elt.val() === ""){ elt.val(value); } }
        if(user){
            setInput(form.$createdBy_name, user.name);
            setInput(form.$createdBy_email, user.email);
            setInput(form.$createdBy_siteUrl, user.siteUrl);
            setInput(form.$createdBy_twitter, user.twitter);
            form.$createdBy_public.prop('checked', user.public);
        }
    }
})();

// look for potential duplicates and show them
(function(){
    var form = Config.Form.Conference;
    if(form.exists() && !form.$id.val()){
        form.$siteUrl.on('change', function(){
            showPotentialDuplicatesIfSome();
        });
        form.$dates.on('change', function(){
            showPotentialDuplicatesIfSome();
        });
    }
    function showPotentialDuplicatesIfSome(){
        Config.Api.getDuplicatedConferences().then(function(conferences){
            renderPotentialDuplicateConferences('duplicate-template', conferences);
        });
    }
    function renderPotentialDuplicateConferences(templateId, conferences){
        var $template = $('#'+templateId);
        var $parent = $template.parent();
        if(conferences.length == 0){
            $parent.hide();
            $('.duplicate-warning').hide();
        } else {
            $parent.show();
            $('.duplicate-warning').show();
            $('.'+templateId).remove();
            conferences.map(function(conference){
                var html = $template.clone().html();
                var formatted = html
                    .replace('{{id}}', conference.id)
                    .replace('{{name}}', conference.name)
                    .replace('{{start}}', moment(conference.start).format('DD/MM/YYYY'))
                    .replace('{{end}}', moment(conference.end).format('DD/MM/YYYY'))
                    .replace('{{location}}', conference.location ? conference.location.formatted : '');
                $parent.append($(formatted).addClass(templateId));
            });
        }
    }
})();
