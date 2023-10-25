var Coral = window.Coral || {},
    Granite = window.Granite || {};

(function (window, document, $, Coral) {
    "use strict";
    $(document).on("foundation-contentloaded", function (e) {

        var SITE_PATH = "/conf/aemoperations/settings/tools/componentandtemplateauditor-initiator.html",
            ui = $(window).adaptTo("foundation-ui");

        if (window.location.href.indexOf(SITE_PATH) < 0) {
            return;
        }

        $(document).off("change", ".close").on("change", ".close", function (event) {
            $(".modal").hide();
        });

        function getValueByName(fieldName, isMandatory) {
            var fieldValue = ($("input[name='" + fieldName + "']").val()).trim();
            if (!isMandatory) {
                return fieldValue;
            }
            if (!fieldValue || fieldValue.length === 0) {
                //for input fields
                $("input[name='" + fieldName + "']").attr('aria-invalid', 'true');
                $("input[name='" + fieldName + "']").attr('invalid', 'invalid');

                //for select fields
                $("coral-select[name='" + fieldName + "']").attr('aria-invalid', 'true');
                $("coral-select[name='" + fieldName + "']").attr('invalid', 'invalid');

                return;
            } else {
                return fieldValue;
            }
        }

        function getValueByNameArea(fieldName, isMandatory) {
            var fieldValue = ($("textarea[name='" + fieldName + "']").val()).trim();
            if (!isMandatory) {
                return fieldValue;
            }
            if (!fieldValue || fieldValue.length === 0) {
                //for input fields
                $("textarea[name='" + fieldName + "']").attr('aria-invalid', 'true');
                $("textarea[name='" + fieldName + "']").attr('invalid', 'invalid');
                //for select fields
                $("coral-select[name='" + fieldName + "']").attr('aria-invalid', 'true');
                $("coral-select[name='" + fieldName + "']").attr('invalid', 'invalid');

                return;
            } else {
                return fieldValue;
            }
        }

        function getFileByName(fieldName) {
            var fieldInput = $("input[name='" + fieldName + "']");
            if(null != fieldInput && null != fieldInput[0]){
                return fieldInput[0].files[0];
            }
        }

        $(document).off("click", ".activation-initiator").on("click", ".activation-initiator", function (event) {
            event.preventDefault();
           // reading text fields and files
           var componentsPath = getValueByName('./componentsPath', false),
           searchPaths = getValueByNameArea('./searchPaths', true);

            var formData = new FormData();
            formData.append("componentsPath", componentsPath);
            formData.append("searchPaths", searchPaths);

            //Add loading Message here
			$(".loading").html("in progress");
            $(".modal").show();

            $.ajax({
                // add the servlet path
                url: "/bin/triggercomponentandtemplateauditor",
                method: "POST",
                async: true,
                cache: false,
                contentType: false,
                processData: false,
                data: formData
            }).done(function (data) {
                if (data && data.length >0) {
                   for (var i = 0; i < data.length; ++i) {
                        $("#component-table-body").append(`<tr class="search-row">
                            <td class="componentPath">${data[i].componentPath}</td>
                            <td class="totalMatchers">${data[i].hasMatches}</td>
                            </tr>`);
                   }
                   $(".modal").hide();
                } else {
                   $(".modal").hide();
                   ui.notify("Error", "Unable to execute the search", "error");
                }
            }).fail(function (data) {
				$(".modal").hide();
                if (data && data.responseJSON && data.responseJSON.message){
                    ui.notify("Error", data.responseJSON.message, "error");
                }else{
                    //add error message
                    ui.notify("Error", "Unable to process request", "error");
                }
            });
        });

        $(document).on("keyup", "input[name='./table-search']", function() {
        var value = $(this).val().toLowerCase();
            $("#component-table-body tr").filter(function() {
              $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1)
            });
        });
    });
})(window, document, $, Coral);
