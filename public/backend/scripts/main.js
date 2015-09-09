$(document).ready(function(){
	/*
	 * Form improvments
	 */
	if($('[data-toggle="tooltip"]')[0]){
		$('[data-toggle="tooltip"]').tooltip();
	}
	if($('.datetimepicker')[0]){
		$('.datetimepicker').datetimepicker();
	}
	if($('.select2')[0]){
		$('.select2').each(function(){
			$(this).select2({
				width: '100%',
				theme: 'bootstrap',
				placeholder: $(this).attr('placeholder'),
				minimumResultsForSearch: 5
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
	}
	if($('.select2-tags')[0]){
		$('.select2-tags').each(function(){
			$(this).select2({
				width: '100%',
				theme: 'bootstrap',
				placeholder: $(this).attr('placeholder'),
				tags: true
			});
		});
	}
	if($('.confirm')[0]){
		$('.confirm').click(function(e){
			var title = $(this).attr('title') || 'Confirmer';
			if(!confirm(title+' ?')){
				e.preventDefault();
			}
		});
	}

	/*
	 * Waves Animation
	 */
	Waves.attach('.btn:not(.btn-icon):not(.btn-float)');
	Waves.attach('.btn-icon, .btn-float', ['waves-circle', 'waves-float']);
	Waves.attach('.main-menu li:not(.sub-menu) > a, .card-event', ['waves-block']); // for sidebar
	Waves.attach('.feature img', ['waves-circle']); // for event features
	Waves.init();

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
	 * Typeahead field
	 */
	$('.typeahead').each(function(){
		var listId = $(this).attr('list');
		var values = [];
		$('datalist#'+listId+' option').each(function(){
			values.push($(this).attr('value'));
		});
		var datasource = new Bloodhound({
			datumTokenizer: Bloodhound.tokenizers.whitespace,
			queryTokenizer: Bloodhound.tokenizers.whitespace,
			local: values
		});
		function datasourceWithDefaults(q, sync){
			if(q === ''){
				sync(values);
			} else {
				datasource.search(q, sync);
			}
		}
		$(this).typeahead({
			minLength: 0,
			highlight: true,
			hint: true
		}, {
			name: 'datalist',
			source: datasourceWithDefaults,
			limit: 15
		});
		$(this).removeAttr('list');
		$('datalist#'+listId).remove();
	});

	/*
	 * HTML Editor
	 */
	$('.wysiwyg').each(function(){
		var wysiwyg = $(this);
		var editor = wysiwyg.find('.editor');
		editor.summernote({
			toolbar: [
				['style', ['style', 'bold', 'italic', 'underline', 'strikethrough']],
				['color', ['color', 'clear']],
				['layout', ['ul', 'ol', 'paragraph']],
				['insert', ['picture', 'link', 'video', 'hr']],
				['actions', ['undo', 'redo']],
				['misc', ['fullscreen', 'codeview', 'help']]
			],
			onBlur: function(e){
				wysiwyg.find('input[type="hidden"]').attr('value', editor.code());
			}
		});
	});

	/*
	 * Toggle Sidebar
	 */
	$('body').on('click', '#menu-trigger', function(e){
		console.log('click')
		e.preventDefault();
		var sourceSelector = '#menu-trigger';
		var targetSelector = $(this).data('trigger');
		$(targetSelector).toggleClass('toggled');
		$(this).toggleClass('open');
		$('body').toggleClass('modal-open');

		// Close opened sub-menus
		$('.sub-menu.toggled').not('.active').each(function(){
			$(this).removeClass('toggled');
			$(this).find('ul').hide();
		});
		$('.profile-menu .main-menu').hide();

		// When clicking outside
		var closeSidebar = function(e){
			if(($(e.target).closest(targetSelector).length === 0) && ($(e.target).closest(sourceSelector).length === 0)){
				setTimeout(function(){
					$('body').removeClass('modal-open');
					$(targetSelector).removeClass('toggled');
					$('#header').removeClass('sidebar-toggled');
					$(sourceSelector).removeClass('open');
					$(document).off('click', closeSidebar);
				});
			}
		};
		$(document).on('click', closeSidebar);
	});

	/*
	 * Toggle Sidebar Profile Menu
	 */
	$('body').on('click', '.profile-menu > a', function(e){
		e.preventDefault();
		$(this).parent().toggleClass('toggled');
		$(this).next().slideToggle(200);
	});

	/*
	 * Toggle Sidebar Submenus
	 */
	$('body').on('click', '.sub-menu > a', function(e){
		e.preventDefault();
		$(this).parent().toggleClass('toggled');
		$(this).next().slideToggle(200);
	});

	/*
	 * Body alt for cards (like todo-list)
	 */
	if($('.card .body-alt')[0]){
		// handle click on button
		$('body').on('click', '.card .body-alt > .body-alt-btn', function(){
			$(this).parent().addClass('toggled');
		});

		// dismiss
		$('body').on('click', '.body-alt.toggled [body-alt-action]', function(e){
			var action = $(this).attr('body-alt-action');
			if(action == 'dismiss'){
				e.preventDefault();
				$(this).closest('.body-alt.toggled').removeClass('toggled');
			}
		});
	}

	/*
	 * Cloudinary upload (cf http://cloudinary.com/documentation/upload_widget#upload_widget_options)
	 */
	$('.cloudinary-upload').each(function(){
		var input = $(this).find('input[type="hidden"]');
		var preview = $(this).find('.preview');
		if(input.attr('value') === ''){ preview.hide(); }
	});
	$('.cloudinary-upload button').click(function(e) {
		e.preventDefault();
		var input = $(this).parent().find('input[type="hidden"]');
		var preview = $(this).parent().find('.preview');

		var ratio = $(this).attr('ratio') || undefined;
		var width = $(this).attr('width') || undefined;
		var height = ratio && width ? Math.round(width/ratio) : undefined;

		cloudinary.openUploadWidget({
			cloud_name: 'saloon',
			upload_preset: 'cfiz5ye2',
			multiple: false,
			cropping: 'server',
			cropping_aspect_ratio: ratio,
			folder: 'backend-images',
			resource_type: '',
			theme: 'minimal',
			show_powered_by: false
		}, function(error, result) {
			if(result && Array.isArray(result) && result.length > 0){
				var args = [];
				if(result[0].coordinates && Array.isArray(result[0].coordinates.custom) && result[0].coordinates.custom.length > 0){
					var coordinates = result[0].coordinates.custom[0];
					args.push({
						mode: 'crop',
						x: coordinates[0],
						y: coordinates[1],
						width: coordinates[2],
						height: coordinates[3]
					});
				}
				/*args.push({
					mode: 'pad',
					width: width,
					height: height
				});*/
				var url = formatCloudinaryUrl(result[0].url, args);
				preview.find('img').attr('src', url);
				preview.show();
				input.attr('value', url);
			}
		});
	});
	function formatCloudinaryUrl(url, args){
		var values = args.map(function(arg){ return buildArg(arg); });
		return url.replace(new RegExp('(.*/upload/).*(v[0-9].*)', 'gi'), '$1'+values.join('/')+'/$2');
	}
	function buildArg(arg){
		var values = [];
		if(typeof arg.mode !== 'undefined'){ values.push('c_'+arg.mode); }
		if(typeof arg.height !== 'undefined'){ values.push('h_'+arg.height); }
		if(typeof arg.width !== 'undefined'){ values.push('w_'+arg.width); }
		if(typeof arg.x !== 'undefined'){ values.push('x_'+arg.x); }
		if(typeof arg.y !== 'undefined'){ values.push('y_'+arg.y); }
		return values.join(',');
	}
	function cleanCloudinaryUrl(url){
		return url.replace(new RegExp('(.*/upload/).*(v[0-9].*)', 'gi'), '$1$2');
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
				x: 20,
				y: 70
			}
		});
	}
});