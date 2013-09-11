var nbTinyMceEditor = 0;

function initTinyMCE(width, height, editorSelector, plugins, lang, toolbar) {
  // force English by default since there are no translations for other
  // languages than en and fr
  if (lang != 'en' && lang != 'fr'){
    lang = 'en';
  }

  nbTinyMceEditor++;

  // if SafeEdit present
  if (typeof registerSafeEditWait == 'function') {
    // tell him to wait until all eleditors are loaded
    registerSafeEditWait(function() {
      return nbTinyMceEditor <= 0;
    });
  }

  // if SafeEdit present
  if (typeof registerPostRestoreCallBacks == 'function') {
     // tell him to run a restore again the following elts
     registerPostRestoreCallBacks(function(formSelector, data) {
        processRestore(jQuery(formSelector).find(
        "textarea.mceEditor,td.mceIframeContainer>iframe"), data);
     }
      );
  }

  tinyMCE.init({
    width : width,
    height : height,
    mode : "specific_textareas",
    theme : "advanced",
    editor_selector : editorSelector,
    editor_deselector : "disableMCEInit",
    plugins : plugins,
    language : lang,
    theme_advanced_resizing : true,

    // Img insertion fixes
    relative_urls : false,
    remove_script_host : false,
    skin : "o2k7",
    skin_variant : "silver",
    theme_advanced_disable : "styleselect",
    theme_advanced_buttons3 : "hr,removeformat,visualaid,|,sub,sup,|,charmap,|",
    theme_advanced_buttons3_add : toolbar,
    setup : function(ed) {
      ed.onInit.add(function(ed) {
          nbTinyMceEditor--;
          // XXX Make sure this work when we have several editors
      });
   }
  });
}

function toggleTinyMCE(id) {
  if (!tinyMCE.getInstanceById(id)) {
    addTinyMCE(id);
  } else {
    removeTinyMCE(id);
  }
}

function removeTinyMCE(id) {
 tinyMCE.execCommand('mceRemoveControl', false, id);
}

function addTinyMCE(id) {
 tinyMCE.execCommand('mceAddControl', false, id);
}

function removeAllTinyMCEEditors() {
  for (var i=0; i < tinyMCE.editors.length; i++) {
     try {
       tinyMCE.execCommand('mceRemoveControl',false, tinymce.editors[i].id);
     } finally {
     }
  };
  return true;
}