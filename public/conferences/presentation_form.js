// look if speaker name already exists to fill other fields
(function(){
    $('input#speakers_0_name').on('change', function(){
        fillSpeakerIfFound($(this))
    });
    function fillSpeakerIfFound($el){
        var name = $el.val();
        console.log('speaker name', name);
        Config.Api.getSpeakers(name).then(function(speakers){
            console.log('speakers', speakers);
        });
    }
})();
