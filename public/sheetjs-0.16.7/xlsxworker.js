/* xlsx.js (C) 2013-present SheetJS -- http://sheetjs.com */
importScripts('/assets/sheetjs-0.16.7/shim.js');
/* uncomment the next line for encoding support */
importScripts('/assets/sheetjs-0.16.7/cpexcel.js');
importScripts('/assets/sheetjs-0.16.7/jszip.js');
importScripts('/assets/sheetjs-0.16.7/xlsx.js');
postMessage({t:"ready"});

onmessage = function (evt) {
  var v;
  try {
    v = XLSX.read(evt.data.d, {type: evt.data.b});
postMessage({t:"xlsx", d:JSON.stringify(v)});
  } catch(e) { postMessage({t:"e",d:e.stack||e}); }
};
