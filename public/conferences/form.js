/*
    generic form scripts
 */

// http://www.daterangepicker.com/
(function(){
    var dateFormat = 'DD/MM/YYYY';
    var datetimeFormat = 'DD/MM/YYYY HH:mm';

    // see common.views.forms.inputDate
    $('input.datepicker').daterangepicker({
        singleDatePicker: true,
        autoUpdateInput: false,
        locale: {
            format: dateFormat
        }
    });
    $('input.datepicker').on('apply.daterangepicker', function(ev, picker) {
        $(this).val(picker.startDate.format(dateFormat));
    });
    $('input.datepicker').on('cancel.daterangepicker', function(ev, picker) {
        $(this).val('');
    });

    // see common.models.utils.DateRange & common.views.forms.inputDateRange
    $('input.daterange').daterangepicker({
        autoUpdateInput: false,
        autoApply: true,
        locale: {
            format: dateFormat
        }
    });
    $('input.daterange').on('apply.daterangepicker', function(ev, picker) {
        $(this).val(picker.startDate.format(dateFormat) + ' - ' + picker.endDate.format(dateFormat));
        $(this).change();
    });
    $('input.daterange').on('cancel.daterangepicker', function(ev, picker) {
        $(this).val('');
    });

    // see common.models.utils.DateRange & common.views.forms.inputDateRange
    $('input.datetimerange').daterangepicker({
        autoUpdateInput: false,
        autoApply: true,
        timePicker: true,
        timePicker24Hour: true,
        locale: {
            format: datetimeFormat
        }
    });
    $('input.datetimerange').on('apply.daterangepicker', function(ev, picker) {
        $(this).val(picker.startDate.format(datetimeFormat) + ' - ' + picker.endDate.format(datetimeFormat));
        $(this).change();
    });
    $('input.datetimerange').on('cancel.daterangepicker', function(ev, picker) {
        $(this).val('');
    });
})();

// http://www.malot.fr/bootstrap-datetimepicker/
(function(){
    if($('input.datetimepicker')[0]){
        $('input.datetimepicker').each(function(){
            var opts = {
                language: 'fr',
                autoclose: true
            };
            var startDate = $(this).attr('startDate');
            if(startDate){ opts.initialDate = startDate; }
            $(this).datetimepicker(opts);
        });
    }
})();

// https://select2.github.io/
(function(){
    if($('.select2')[0]){
        $('.select2').each(function(){
            $(this).select2({
                width: '100%',
                theme: 'bootstrap',
                placeholder: $(this).attr('placeholder'),
                allowClear: true
            }).select2("val", ""); // no value at start
        });
    }

    if($('.select2-tags')[0]){
        $('.select2-tags').each(function(){
            $(this).select2({
                width: '100%',
                theme: 'bootstrap',
                placeholder: $(this).attr('placeholder'),
                tags: true,
                tokenSeparators: [',']
            });
        });
    }

    if($('.select2-tags-item')[0]){
        function buildTemplate(text){
            return function(item){
                if(item.element){
                    return $('<span>'+$(item.element).attr('template')+'</span>');
                } else {
                    item.id = 'new';
                    return text || item.text;
                }
            };
        }
        $('.select2-tags-item').each(function(){
            var $select = $(this);
            $select.select2({
                width: '100%',
                theme: 'bootstrap',
                placeholder: $select.attr('placeholder'),
                tags: true,
                templateResult: buildTemplate($select.attr('onCreateLabel')),
                templateSelection: buildTemplate($select.attr('onCreateLabel'))
            });
            var onCreate = $select.attr('onCreate');
            if(onCreate){
                $select.on('select2:select', function(evt){
                    if(typeof window[onCreate] === 'function'){
                        if(evt && evt.params && evt.params.data && evt.params.data.id === 'new') {
                            window[onCreate]($select, evt);
                        }
                    }
                });
            }
        });
    }
})();

// inputImgUrl
(function(){
    $('.img-url-picker').each(function() {
        var $elt = $(this);
        var $input = $elt.find('input[type="text"]');
        var $preview = $elt.find('img.preview');
        $input.on('change', function(){
            update($input, $preview);
        });
        update($input, $preview); // run on page load
    });
    function update($input, $preview){
        if($input.val() != ''){
            $preview.attr('src', $input.val());
            $preview.show();
        } else {
            $preview.hide();
        }
    }
})();

// fill img url with twitter account (input having twitterToImg attribute pointing to imgUrl field id)
(function(){
    $('input[twitterToImg]').each(function(){
        var $twitterAccountField = $(this);
        var $imgUrlField = $('#'+$twitterAccountField.attr('twitterToImg'));
        $twitterAccountField.on('change', function () {
            update($twitterAccountField, $imgUrlField);
        });
        update($twitterAccountField, $imgUrlField); // run on page load
    });
    function update($twitterAccountField, $imgUrlField){
        if ($twitterAccountField.val() !== '' && $imgUrlField.val() === '') {
            Config.Api.getTwitterAccount($twitterAccountField.val()).then(function (account) {
                if (account && $imgUrlField.val() === '') {
                    $imgUrlField.val(account.avatar).change();
                }
            });
        }
    }
})();

// GMapPlace picker (https://developers.google.com/maps/documentation/javascript/examples/places-autocomplete?hl=fr)
function initPlacePicker(){
    $('.gmap-place-picker').each(function() {
        var $elt = $(this);
        var $input = $elt.find('input[type="text"]');
        var mapData = initMap($elt);
        var autocomplete = new google.maps.places.Autocomplete($input.get(0));
        autocomplete.addListener('place_changed', function() {
            var place = autocomplete.getPlace(); // cf https://developers.google.com/maps/documentation/javascript/3.exp/reference?hl=fr#PlaceResult
            update($elt, mapData, toLocation(place));
        });
        $input.on('change', function(){
            if($input.val() === ''){
                update($elt, mapData, null);
            }
        });
        update($elt, mapData, readForm($elt)); // run on page load
    });

    function update($elt, mapData, location){
        writeForm($elt, location);
        if(location && location.geo && location.geo.lat){
            showMap(mapData, location);
        } else {
            hideMap(mapData);
        }
    }
    function initMap($elt){
        var $map = $elt.find('.map');
        var map = new google.maps.Map($map.get(0), {
            center: {lat: -33.8688, lng: 151.2195},
            zoom: 13
        });
        var marker = new google.maps.Marker({
            map: map,
            anchorPoint: new google.maps.Point(0, -29)
        });
        var infowindow = new google.maps.InfoWindow();
        return {
            $map: $map,
            map: map,
            marker: marker,
            infowindow: infowindow
        };
    }
    function showMap(mapData, formattedPlace){
        mapData.$map.show();
        google.maps.event.trigger(mapData.map, 'resize');
        mapData.infowindow.close();
        mapData.marker.setVisible(false);
        mapData.map.setCenter(formattedPlace.geo);
        mapData.map.setZoom(15);
        mapData.marker.setPosition(formattedPlace.geo);
        mapData.marker.setVisible(true);
        mapData.infowindow.setContent(
            '<strong>'+formattedPlace.name+'</strong><br>'+
            formattedPlace.streetNo+' '+formattedPlace.street+'<br>'+
            formattedPlace.postalCode+' '+formattedPlace.locality+', '+formattedPlace.country
        );
        mapData.infowindow.open(mapData.map, mapData.marker);
    }
    function hideMap(mapData){
        mapData.$map.hide();
    }
    function writeForm($elt, formattedPlace){
        $elt.find('input[type="hidden"].place-id').val(formattedPlace ? formattedPlace.id : '');
        $elt.find('input[type="hidden"].place-name').val(formattedPlace ? formattedPlace.name : '');
        $elt.find('input[type="hidden"].place-streetNo').val(formattedPlace ? formattedPlace.streetNo : '');
        $elt.find('input[type="hidden"].place-street').val(formattedPlace ? formattedPlace.street : '');
        $elt.find('input[type="hidden"].place-postalCode').val(formattedPlace ? formattedPlace.postalCode : '');
        $elt.find('input[type="hidden"].place-locality').val(formattedPlace ? formattedPlace.locality : '');
        $elt.find('input[type="hidden"].place-country').val(formattedPlace ? formattedPlace.country : '');
        $elt.find('input[type="hidden"].place-formatted').val(formattedPlace ? formattedPlace.formatted : '');
        $elt.find('input[type="hidden"].place-lat').val(formattedPlace ? formattedPlace.geo.lat : '');
        $elt.find('input[type="hidden"].place-lng').val(formattedPlace ? formattedPlace.geo.lng : '');
        $elt.find('input[type="hidden"].place-url').val(formattedPlace ? formattedPlace.url : '');
        $elt.find('input[type="hidden"].place-website').val(formattedPlace ? formattedPlace.website : '');
        $elt.find('input[type="hidden"].place-phone').val(formattedPlace ? formattedPlace.phone : '');
    }
    function readForm($elt){
        return {
            id: $elt.find('input[type="hidden"].place-id').val(),
            name: $elt.find('input[type="hidden"].place-name').val(),
            streetNo: $elt.find('input[type="hidden"].place-streetNo').val(),
            street: $elt.find('input[type="hidden"].place-street').val(),
            postalCode: $elt.find('input[type="hidden"].place-postalCode').val(),
            locality: $elt.find('input[type="hidden"].place-locality').val(),
            country: $elt.find('input[type="hidden"].place-country').val(),
            formatted: $elt.find('input[type="hidden"].place-formatted').val(),
            geo: {
                lat: parseFloat($elt.find('input[type="hidden"].place-lat').val()),
                lng: parseFloat($elt.find('input[type="hidden"].place-lng').val())
            },
            url: $elt.find('input[type="hidden"].place-url').val(),
            website: $elt.find('input[type="hidden"].place-website').val(),
            phone: $elt.find('input[type="hidden"].place-phone').val()
        };
    }
    function toLocation(place){
        function of(elt, field, defaultValue){ return elt && elt[field] ? elt[field] : (defaultValue ? defaultValue : ''); }
        function formatAddressComponents(components){
            function findByType(components, type){
                var c = components.find(function(c){ return c.types.indexOf(type) >= 0; });
                return c ? c.long_name : undefined;
            }
            return {
                street_number: findByType(components, "street_number"), // ex: "119"
                route: findByType(components, "route"), // ex: "Boulevard Voltaire"
                postal_code: findByType(components, "postal_code"), // ex: "75011"
                locality: findByType(components, "locality"), // ex: "Paris"
                country: findByType(components, "country"), // ex: "France"
                administrative_area: {
                    level_1: findByType(components, "administrative_area_level_1"), // ex: "Île-de-France"
                    level_2: findByType(components, "administrative_area_level_2"), // ex: "Paris"
                    level_3: findByType(components, "administrative_area_level_3"),
                    level_4: findByType(components, "administrative_area_level_4"),
                    level_5: findByType(components, "administrative_area_level_5")
                },
                sublocality: {
                    level_1: findByType(components, "sublocality_level_1"),
                    level_2: findByType(components, "sublocality_level_2"),
                    level_3: findByType(components, "sublocality_level_3"),
                    level_4: findByType(components, "sublocality_level_4"),
                    level_5: findByType(components, "sublocality_level_5")
                }
            };
        }
        var components = formatAddressComponents(place.address_components);
        var loc = place && place.geometry ? place.geometry.location : undefined;
        return {
            id: of(place, 'place_id'),
            name: of(place, 'name'),
            streetNo: of(components, 'street_number'),
            street: of(components, 'route'),
            postalCode: of(components, 'postal_code'),
            locality: of(components, 'locality'),
            country: of(components, 'country'),
            formatted: of(place, 'formatted_address'),
            geo: {
                lat: loc ? loc.lat() : '',
                lng: loc ? loc.lng() : ''
            },
            url: of(place, 'url'),
            website: of(place, 'website'),
            phone: of(place, 'international_phone_number')
        };
    }
}
