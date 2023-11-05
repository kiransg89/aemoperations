(function($, $document) {

    $(document).on("foundation-contentloaded", function(e) {
        var value = $("coral-select[name='./country']").val();
        var compPath = $("coral-select[name='./country']").attr("data-component-path");
        populateItems(compPath, value);
    });

    var REGEX_SELECTOR = "dropdown.selector",
        foundationReg = $(window).adaptTo("foundation-registry");

    foundationReg.register("foundation.validation.validator", {
        selector: "[data-foundation-validation='" + REGEX_SELECTOR + "']",
        validate: function(el) {
            if ($(el).is(":visible")) {
                var value = $(el).val();
                var flag = false;
                var compPath = $(el).attr("data-component-path");
                var errorMessage = 'Error occurred during processing';
                populateItems(compPath, value);
            }
        }
    });

    function populateItems(compPath, value) {
        $.ajax({
            type: "GET",
            async: false,
            url: "/bin/electronicsServlet?componentPath=" + compPath + "&dropdownValue=" + value,
            success: function(result) {
                if (result) {
                    var select = document.querySelector("coral-select[name='./devices']");
                    Coral.commons.ready(select, function(component) {
                        component.items.clear();
                        for (var i = 0; i < result.length; ++i) {
                            var option = document.createElement('coral-select-item');
                            option.textContent = result[i].text;
                            option.value = result[i].value;
                            component.items.add(option);
                        }
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

}(jQuery, $(document)));