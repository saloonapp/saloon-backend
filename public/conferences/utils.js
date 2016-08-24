var Utils = (function(){
    return {
        getSafe: getSafe,
        setSafe: setSafe,
        isUrl: isUrl,
        unique: unique
    };
    function unique(arr, fn){
        var found = {};
        var result = [];
        for(var i=0; i<arr.length; i++){
            if(!found[fn(arr[i])]){
                found[fn(arr[i])] = true;
                result.push(arr[i]);
            }
        }
        return result;
    }
    function getSafe(obj, path) {
        if(typeof path === 'string')  { return getSafe(obj, path.split('.').filter(function(e){ return !!e; })); }
        if(!Array.isArray(path))      { return obj; }
        if(path.length === 0 || !obj) { return obj; }
        var newObj = obj[path[0]];
        var newPath = path.slice(1);
        return getSafe(newObj, newPath);
    }
    function setSafe(obj, path, value) {
        if(typeof path === 'string')                    { return setSafe(obj, path.split('.').filter(function(e){ return !!e; }), value); }
        if(!Array.isArray(path) || path.length === 0)   { return obj; }
        if(path.length === 1){
            obj[path[0]] = value;
            return obj;
        } else {
            var newObj = obj[path[0]] || {};
            obj[path[0]] = newObj;
            var newPath = path.slice(1);
            return setSafe(newObj, newPath, value);
        }
    }
    function isUrl(str) {
        var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
        return regexp.test(str);
    }
})();

var Storage = (function(){
    return {
        get: get,
        set: set
    };
    function get(key){
        if(localStorage){
            var json = localStorage.getItem(key);
            try {
                return JSON.parse(json || '{}');
            } catch(e) {
                console.warn('Unable to parse to JSON', json);
            }
        }
    }
    function set(key, value){
        if(localStorage && value){
            localStorage.setItem(key, JSON.stringify(value));
        }
    }
})();
