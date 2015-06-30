$(function() {
	$('[data-toggle="tooltip"]').tooltip();
	$('.datetimepicker').datetimepicker();
	$('.select2').each(function(){
		$(this).select2({
			theme: 'bootstrap',
			placeholder: $(this).attr('placeholder')
		});
	});
	$('.select2-tags').each(function(){
		$(this).select2({
			theme: 'bootstrap',
			placeholder: $(this).attr('placeholder'),
			tags: true
		});
	});
	$('.confirm').click(function(e){
		if(!confirm('Sure ?')){
			e.preventDefault();
		}
	});

	// http://cloudinary.com/documentation/upload_widget#upload_widget_options
	$('.cloudinary-upload button').click(function(e) {
		e.preventDefault();
		var thumbnail = $(this).next();
		var input = $(this).next().next();

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
				args.push({
					mode: 'pad',
					width: width,
					height: height
				});
				var url = formatCloudinaryUrl(result[0].url, args);
				thumbnail.attr('src', url);
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
});
