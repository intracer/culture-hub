/**
 * Initialize elements based on classes
 */
function initializeElements() {
    $(".saveButton").button();
}


/**
 * Post knockoutJS form data as JSON string, into a parameter called "data"
 * 
 * @param url the URL to submit to
 * @param viewModel the data object
 * @param onSuccess the success callback to run after successful execution
 * @param onFailure the failure callback to run on error
 */
$.postKOJson = function (url, viewModel, onSuccess, onFailure) {
    return jQuery.ajax({
        type: 'POST',
        url: url,
        data: {data: ko.toJSON(viewModel) },
        contentType: 'application/x-www-form-urlencoded; charset=utf-8',
        dataType: 'json'
    }).success(onSuccess).error(onFailure);
};


/**
 * KnockoutJS binder for the jQuery tokenInput plugin
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
ko.bindingHandlers.tokens = {
    init: function(element, valueAccessor, allBindingsAccessor, viewModel) {
        viewModel.lock = false;
        var modelValue = valueAccessor();

        $(element).change(function() {
            if (!viewModel.lock) {
                var tokens = $(element).tokenInput('get');
                if (typeof tokens !== 'undefined') {
                    if (ko.isWriteableObservable(modelValue)) {
                        var existing = ko.utils.unwrapObservable(modelValue);
                        var existingNames = [];
                        $.each(existing, function(index, el) {
                            existingNames.push(typeof el.name == 'function' ? el.name() : el.name);
                        });
                        var updatedNames = [];
                        $.each(tokens, function(index, el) {
                            updatedNames.push(el.name);
                        });

                        // remove elements
                        $.each(existing, function(index, el) {
                            var name = typeof el.name == 'function' ? el.name() : el.name;
                            if ($.inArray(name, updatedNames) < 0) {
                                modelValue.remove(el);
                                tokens.splice(index, 1);
                            }
                        });
                        // add new
                        $.each(tokens, function(index, el) {
                            if ($.inArray(el.name, existingNames) < 0) {
                                modelValue.push({id: el.id, name: el.name});
                            }
                        });
                    }

                }
            }
        });


    },
    update:
            function(element, valueAccessor, allBindingsAccessor, viewModel) {
                viewModel.lock = true;
                $(element).tokenInput('clear');
                var value = valueAccessor();
                var tokens = ko.utils.unwrapObservable(value);
                $(tokens).each(function() {
                    var token = ko.utils.unwrapObservable(this);
                    $(element).tokenInput('add', {id: typeof token.id == 'function' ? token.id() : token.id, name: typeof token.name == 'function' ? token.name() : token.name});
                });
                delete viewModel.lock;
            }
};