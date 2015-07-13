$(document).ready(function(){
	/*
	 * Form improvments
	 */
	$('.select2').each(function(){
		$(this).select2({
			theme: 'bootstrap',
			minimumResultsForSearch: 5,
			placeholder: $(this).attr('placeholder')
		});
		// Add blue animated border and remove depending on value
		$(this).on('select2:select', function(e){
			var p = $(this).closest('.form-group');
			var i = p.find('.form-control').val();
			if(i.length > 0){
				$(this).closest('.fg-line').addClass('fg-toggled');
			} else if(p.hasClass('fg-float')){
				if(i.length == 0){
					$(this).closest('.fg-line').removeClass('fg-toggled');
				}
			} else {
				$(this).closest('.fg-line').removeClass('fg-toggled');
			}
		});
	});

	/*
	 * Text Field
	 */
	// Add blue animated border and remove with condition when focus and blur
	if($('.fg-line')[0]){
		$('body').on('focus', '.form-control', function(){
			$(this).closest('.fg-line').addClass('fg-toggled');
		});
		$('body').on('blur', '.form-control', function(){
			var p = $(this).closest('.form-group');
			var i = p.find('.form-control').val();
			if(p.hasClass('fg-float')){
				if(i.length == 0){
					$(this).closest('.fg-line').removeClass('fg-toggled');
				}
			} else {
				$(this).closest('.fg-line').removeClass('fg-toggled');
			}
		});
	}
	// Add blue border for pre-valued fg-flot text fields
	if($('.fg-float')[0]){
		$('.fg-float .form-control').each(function(){
			var i = $(this).val();
			if(!i.length == 0){
				$(this).closest('.fg-line').addClass('fg-toggled');
			}
		});
	}

	/*
	 * IE 9 Placeholder
	 */
	if($('html').hasClass('ie9')) {
		$('input, textarea').placeholder({
			customClass: 'ie9-placeholder'
		});
	}

	/*
	 * Notifications (flash messages)
	 */
	$('.notification-container .alert').each(function(){
		var message = $(this).find('span.message').text();
		var type = $(this).hasClass('alert-success') ? 'success' : 'inverse';
		notify(message, type);
	});
	function notify(message, type){
		$.growl({
			message: message
		},{
			type: type,
			allow_dismiss: true,
			label: 'Cancel',
			className: 'btn-xs btn-inverse',
			placement: {
				from: 'top',
				align: 'right'
			},
			delay: 25000,
			animate: {
				enter: 'animated bounceIn',
				exit: 'animated bounceOut'
			},
			offset: {
				x: 30,
				y: 90
			}
		});
	}
});