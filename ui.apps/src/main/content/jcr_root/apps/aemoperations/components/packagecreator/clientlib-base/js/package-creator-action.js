var Coral = window.Coral || {},
    Granite = window.Granite || {};

(function (window, document, $, Coral) {
    "use strict";
    $(document).on("foundation-contentloaded", function (e) {

        var SITE_PATH = "/conf/aemoperations/settings/tools/create-package-initiator.html",
            ui = $(window).adaptTo("foundation-ui");

        if (window.location.href.indexOf(SITE_PATH) < 0) {
            return;
        }

        $(document).off("change", ".close").on("change", ".close", function (event) {
            $(".modal").hide();
            getStatus(false);
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

        $(document).off("click", ".package-initiator").on("click", ".package-initiator", function (event) {
            event.preventDefault();

            var packageName = getValueByName('./packageName', true),
                packageDescription = getValueByName('./packageDescription', true),
                pathsList = getValueByNameArea('./pathsList', true),
                getChildren = getValueByName('./getChildren', true);

            var formData = new FormData();
            formData.append("packageName", packageName);
            formData.append("packageDescription", packageDescription);
            formData.append("pathsList", pathsList);
            formData.append("getChildren", getChildren);

			$(".loading").html("PLEASE WAIT CREATINGING PACKAGE");
            $(".modal").show();
            getStatus(true);

            $.ajax({
                url: "/bin/triggerPackageCreator",
                method: "POST",
                async: true,
                cache: false,
                contentType: false,
                processData: false,
                data: formData
            }).done(function (data) {
                if (data && data.message){
                    ui.notify("Success", data.message, "success");
                    var dialog = new Coral.Dialog();
                    dialog.id = 'dialogSuccess';
                    dialog.header.innerHTML = 'Success';
                    dialog.content.innerHTML = data.message;
                    dialog.footer.innerHTML = '<button class="ok-button" is="coral-button" variant="primary" icon="check" coral-close>OK</button>';
                    dialog.variant = 'success';
                    dialog.closable = "on";
                    dialog.show();
                    $(".modal").hide();
                    getStatus(false);
                }else{
                    ui.notify("Error", "Unable to create package ", "error");
                }
            }).fail(function (data) {
				$(".modal").hide();
				getStatus(false);
                if (data && data.responseJSON && data.responseJSON.message){
                    ui.notify("Error", data.responseJSON.message, "error");
                }else{
                    ui.notify("Error", "Unable to create package", "error");
                }
            });
        });

       function getStatus(showStatus) {
         $.ajax({
            // add the servlet path
            url: "/bin/cpustatust",
            method: "GET",
            async: true,
            cache: false,
            contentType: false,
            processData: false
         }).done(function (data) {
            if (data) {
               data = JSON.parse(data);
               $("#table-body").append(`<tr>
						<td>${data.cpu}/${data.maxCpu}</td>
			            <td>${data.mem}/${data.maxMem}</td>
		           		 </tr>`);
            } else {
               $(".modal").hide();
               showStatus = false;
               ui.notify("Error", "Unable to get the status", "error");
            }
         }).fail(function (data) {
            $(".modal").hide();
            showStatus = false;
            if (data && data.responseJSON && data.responseJSON.message) {
               ui.notify("Error", data.responseJSON.message, "error");
            } else {
               //add error message
               ui.notify("Error", "Unable to get the status", "error");
            }
         });
         if (showStatus) {
            setTimeout(() => {
               emptyResults();
               getStatus(true);
            }, 2000);
         }
      }

    function emptyResults() {
      $("#table-body").empty();
    }
    });
})(window, document, $, Coral);
