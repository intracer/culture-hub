// Knockout Mapping plugin v1.2
// (c) 2011 Steven Sanderson, Roy Jacobs - http://knockoutjs.com/
// License: Ms-Pl (http://www.opensource.org/licenses/ms-pl.html)

ko.exportSymbol=function(m,q){for(var j=m.split("."),k=window,h=0;h<j.length-1;h++)k=k[j[h]];k[j[j.length-1]]=q};ko.exportProperty=function(m,q,j){m[q]=j};
(function(){function m(a,c){for(var b in c)c.hasOwnProperty(b)&&c[b]&&(a[b]=c[b])}function q(a,c){var b={};m(b,a);m(b,c);return b}function j(a){if(a&&typeof a==="object"&&a.constructor==(new Date).constructor)return"date";return typeof a}function k(a,c){a=a||{};if(a.create instanceof Function||a.key instanceof Function||a.arrayChanged instanceof Function)a={"":a};if(c)a.ignore=h(c.ignore,a.ignore),a.include=h(c.include,a.include),a.copy=h(c.copy,a.copy);a.ignore=h(a.ignore,g.ignore);a.include=h(a.include,
g.include);a.copy=h(a.copy,g.copy);a.mappedProperties={};return a}function h(a,c){a instanceof Array||(a=j(a)==="undefined"?[]:[a]);c instanceof Array||(c=j(c)==="undefined"?[]:[c]);return a.concat(c)}function B(){ko.dependentObservable=function(a,c,b){b=b||{};b.deferEvaluation=!0;a=new w(a,c,b);a.__ko_proto__=w;return a}}function s(a,c,b,e,d,h,t){var g=ko.utils.unwrapObservable(c)instanceof Array,t=t||"";if(ko.mapping.isMapped(a))var i=ko.utils.unwrapObservable(a)[l],b=q(i,b);i=function(){return b[d]&&
b[d].create instanceof Function};e=e||new C;if(e.get(c))return a;d=d||"";if(g){var g=[],f=function(a){return a};if(b[d]&&b[d].key)f=b[d].key;var m=function(a){return a};i()&&(m=function(a){return b[d].create({data:a,parent:h})});if(!ko.isObservable(a))a=ko.observableArray([]),a.mappedRemove=function(b){var c=typeof b=="function"?b:function(a){return a===f(b)};return a.remove(function(a){return c(f(a))})},a.mappedRemoveAll=function(b){var c=u(b,f);return a.remove(function(a){return ko.utils.arrayIndexOf(c,
f(a))!=-1})},a.mappedDestroy=function(b){var c=typeof b=="function"?b:function(a){return a===f(b)};return a.destroy(function(a){return c(f(a))})},a.mappedDestroyAll=function(b){var c=u(b,f);return a.destroy(function(a){return ko.utils.arrayIndexOf(c,f(a))!=-1})},a.mappedIndexOf=function(b){var c=u(a(),f),b=f(b);return ko.utils.arrayIndexOf(c,b)},a.mappedCreate=function(b){if(a.mappedIndexOf(b)!==-1)throw Error("There already is an object with the key that you specified.");b=m(b);a.push(b);return b};
for(var i=u(ko.utils.unwrapObservable(a),f).sort(),k=u(c,f).sort(),i=ko.utils.compareArrays(i,k),k={},p=[],x=0,z=i.length;x<z;x++){var r=i[x],n,o=t+"["+x+"]";switch(r.status){case "added":var v=y(ko.utils.unwrapObservable(c),r.value,f);n=ko.utils.unwrapObservable(s(void 0,v,b,e,d,a,o));o=D(ko.utils.unwrapObservable(c),v,k);p[o]=n;k[o]=!0;break;case "retained":v=y(ko.utils.unwrapObservable(c),r.value,f);n=y(a,r.value,f);s(n,v,b,e,d,a,o);o=D(ko.utils.unwrapObservable(c),v,k);p[o]=n;k[o]=!0;break;case "deleted":n=
y(a,r.value,f)}g.push({event:r.status,item:n})}a(p);b[d]&&b[d].arrayChanged&&ko.utils.arrayForEach(g,function(a){b[d].arrayChanged(a.event,a.item)})}else if(A(c)){if(!a)if(i())return B(),n=b[d].create({data:c,parent:h}),ko.dependentObservable=w,n;else a={};e.save(c,a);E(c,function(d){var f=t.length?t+"."+d:d;if(b.ignore.indexOf(f)==-1)if(b.copy.indexOf(f)!=-1)a[d]=c[d];else{var g=e.get(c[d]);a[d]=g?g:s(a[d],c[d],b,e,d,a,f);b.mappedProperties[f]=!0}})}else switch(j(c)){case "function":a=c;break;default:ko.isWriteableObservable(a)?
a(ko.utils.unwrapObservable(c)):i()?(B(),a=b[d].create({data:c,parent:h}),ko.dependentObservable=w):a=ko.observable(ko.utils.unwrapObservable(c))}return a}function D(a,c,b){for(var e=0,d=a.length;e<d;e++)if(b[e]!==!0&&a[e]==c)return e;return null}function z(a,c){var b;c&&(b=c(a));j(b)==="undefined"&&(b=a);return ko.utils.unwrapObservable(b)}function y(a,c,b){a=ko.utils.arrayFilter(ko.utils.unwrapObservable(a),function(a){return z(a,b)==c});if(a.length==0)throw Error("When calling ko.update*, the key '"+
c+"' was not found!");if(a.length>1&&A(a[0]))throw Error("When calling ko.update*, the key '"+c+"' was not unique!");return a[0]}function u(a,c){return ko.utils.arrayMap(ko.utils.unwrapObservable(a),function(a){return c?z(a,c):a})}function E(a,c){if(a instanceof Array)for(var b=0;b<a.length;b++)c(b);else for(b in a)c(b)}function A(a){var c=j(a);return c=="object"&&a!==null&&c!=="undefined"}function C(){var a=[],c=[];this.save=function(b,e){var d=ko.utils.arrayIndexOf(a,b);d>=0?c[d]=e:(a.push(b),c.push(e))};
this.get=function(b){b=ko.utils.arrayIndexOf(a,b);return b>=0?c[b]:void 0}}ko.mapping={};var l="__ko_mapping__",w=ko.dependentObservable,p={include:["_destroy"],ignore:[],copy:[]},g=p;ko.mapping.fromJS=function(a,c,b){if(arguments.length==0)throw Error("When calling ko.fromJS, pass the object you want to convert.");var c=k(c),e=s(b,a,c);e[l]=q(e[l],c);return e};ko.mapping.fromJSON=function(a,c){var b=ko.utils.parseJson(a);return ko.mapping.fromJS(b,c)};ko.mapping.isMapped=function(a){return(a=ko.utils.unwrapObservable(a))&&
a[l]};ko.mapping.updateFromJS=function(a,c){if(arguments.length<2)throw Error("When calling ko.updateFromJS, pass: the object to update and the object you want to update from.");if(!a)throw Error("The object is undefined.");if(!a[l])throw Error("The object you are trying to update was not created by a 'fromJS' or 'fromJSON' mapping.");return s(a,c,a[l])};ko.mapping.updateFromJSON=function(a,c,b){c=ko.utils.parseJson(c);return ko.mapping.updateFromJS(a,c,b)};ko.mapping.toJS=function(a,c){g||ko.mapping.resetDefaultOptions();
if(arguments.length==0)throw Error("When calling ko.mapping.toJS, pass the object you want to convert.");if(!(g.ignore instanceof Array))throw Error("ko.mapping.defaultOptions().ignore should be an array.");if(!(g.include instanceof Array))throw Error("ko.mapping.defaultOptions().include should be an array.");if(!(g.copy instanceof Array))throw Error("ko.mapping.defaultOptions().copy should be an array.");c=k(c,a[l]);return ko.mapping.visitModel(a,function(a){return ko.utils.unwrapObservable(a)},
c)};ko.mapping.toJSON=function(a,c){var b=ko.mapping.toJS(a,c);return ko.utils.stringifyJson(b)};ko.mapping.defaultOptions=function(){if(arguments.length>0)g=arguments[0];else return g};ko.mapping.resetDefaultOptions=function(){g={include:p.include.slice(0),ignore:p.ignore.slice(0),copy:p.copy.slice(0)}};ko.mapping.visitModel=function(a,c,b){b=b||{};b.visitedObjects=b.visitedObjects||new C;var e,d=ko.utils.unwrapObservable(a);if(A(d))c(a,b.parentName),e=d instanceof Array?[]:{};else return c(a,b.parentName);
b.visitedObjects.save(a,e);var g=b.parentName;E(d,function(a){if(!(b.ignore&&ko.utils.arrayIndexOf(b.ignore,a)!=-1)){var h=d[a],i=b,f=g||"";d instanceof Array?g&&(f+="["+a+"]"):(g&&(f+="."),f+=a);i.parentName=f;if(!(ko.utils.arrayIndexOf(b.copy,a)===-1&&ko.utils.arrayIndexOf(b.include,a)===-1&&d[l]&&d[l].mappedProperties&&!d[l].mappedProperties[a]&&!(d instanceof Array)))switch(j(ko.utils.unwrapObservable(h))){case "object":case "undefined":i=b.visitedObjects.get(h);e[a]=j(i)!=="undefined"?i:ko.mapping.visitModel(h,
c,b);break;default:e[a]=c(h,b.parentName)}}});return e};ko.exportSymbol("ko.mapping",ko.mapping);ko.exportSymbol("ko.mapping.fromJS",ko.mapping.fromJS);ko.exportSymbol("ko.mapping.fromJSON",ko.mapping.fromJSON);ko.exportSymbol("ko.mapping.isMapped",ko.mapping.isMapped);ko.exportSymbol("ko.mapping.defaultOptions",ko.mapping.defaultOptions);ko.exportSymbol("ko.mapping.toJS",ko.mapping.toJS);ko.exportSymbol("ko.mapping.toJSON",ko.mapping.toJSON);ko.exportSymbol("ko.mapping.updateFromJS",ko.mapping.updateFromJS);
ko.exportSymbol("ko.mapping.updateFromJSON",ko.mapping.updateFromJSON);ko.exportSymbol("ko.mapping.visitModel",ko.mapping.visitModel)})();
