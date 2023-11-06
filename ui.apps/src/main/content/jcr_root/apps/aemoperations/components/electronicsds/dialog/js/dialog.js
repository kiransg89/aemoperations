(function($, $document, Granite, Coral) {

    var flag = false;
    var errorMessage = 'Error occurred during processing';

    $(document).on("foundation-contentloaded", function(e) {
        var value = $("coral-select[name='./country']").val();
        var fieldPath = $("coral-select[name='./devices']").attr("data-field-path");
        var compPath = $("coral-select[name='./devices']").attr("data-component-path");
        populateItems(fieldPath, compPath, value);
    });

    var REGEX_SELECTOR = "dropdown.selector",
        foundationReg = $(window).adaptTo("foundation-registry");

    foundationReg.register("foundation.validation.validator", {
        selector: "[data-foundation-validation='" + REGEX_SELECTOR + "']",
        validate: function(el) {
            if ($(el).is(":visible")) {
                var value = $(el).val();
                var fieldPath = $("coral-select[name='./devices']").attr("data-field-path");
                var compPath = $("coral-select[name='./devices']").attr("data-component-path");
                populateItems(fieldPath, compPath, value);
            }
        }
    });

    function populateItems(fieldPath, compPath, value) {
        dsUrl = Granite.HTTP.externalize(fieldPath) + ".html?componentPath=" + compPath + "&dropdownValue=" + value;
        $.ajax({
            type: "GET",
            async: false,
            url: dsUrl,
            success: function(result) {
                if (result) {
                        var select = document.querySelector("coral-select[name='./devices']");
                        var newHtml = document.createElement("div");
                        newHtml.innerHTML = result;
                        var newResult = newHtml.querySelector("coral-select[name='./devices']").children;
                        Coral.commons.ready(select, function(component) {
                            component.items.clear();
                            [...newResult].forEach((e) => {
                              component.items.add(e);
                            })
                        });
                    } else {
                        flag = true;
                    }
                }
        });
        if (flag) {
            return errorMessage;
        }
    }

}(jQuery, $(document), Granite, Coral));