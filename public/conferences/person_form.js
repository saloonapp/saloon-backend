var createPersonModal = (function(){
    var $modal = $('#create-person-modal');

    $modal.find('.save').on('click', function(){
        var person = formToPerson($modal);
        Config.Api.createPerson(person).then(function(createdPerson){
            addToSelect($modal.data('$select'), createdPerson);
            closeModal($modal);
        }, function(err){
            alert('ERROR '+err.status+' '+err.statusText+' :\n'+JSON.stringify(err.responseJSON));
        });
    });

    return function($select, evt){
        $modal.data('$select', $select);
        openModal($modal, evt.params.data.text);
    };

    function formToPerson(form){
        var person = {};
        form.find('input').each(function(){
            var value = $(this).attr('type') === 'checkbox' ? $(this).prop('checked') : $(this).val();
            if(value !== ''){
                Utils.setSafe(person, $(this).attr('name'), value);
            }
        });
        return person;
    }
    function cleanForm(form){
        form.find('input').each(function(){
            if(!$(this).attr('name').startsWith('createdBy')){
                $(this).val('').change();
            }
        });
    }
    function openModal(modal, text){
        cleanForm(modal);
        modal.find('input[name=name]').val(text);
        modal.modal('show');
    }
    function closeModal(modal){
        modal.modal('hide');
        cleanForm(modal);
    }
    function addToSelect(select, person){
        var template = (person.avatar ? "<img src='"+person.avatar+"' style='height: 18px;'> " : "") + person.name;
        select.append('<option value="'+person.id+'" template="'+template+'" selected>'+person.name+'</option>');
        select.trigger('change');
    }
})();
