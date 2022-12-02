var Coral = window.Coral || {},
    Granite = window.Granite || {};

(function (window, document, $, Coral) {
    "use strict";
    var packageOperation = "UPLOAD";
    $(document).on("foundation-contentloaded", function (e) {

        var SITE_PATH = "/conf/aemoperations/settings/tools/packagehandler-initiator.html",
            ui = $(window).adaptTo("foundation-ui");

        if (window.location.href.indexOf(SITE_PATH) < 0) {
            return;
        }

        if (packageOperation === "UPLOAD") {
            $("input[name='./packageName']").attr("disabled", true);
            $("input[name='./packageGroup']").attr("disabled", true);
            $("input[name='./packageVersion']").attr("disabled", true);
        }

        $(document).off("change", ".coral-RadioGroup").on("change", ".coral-RadioGroup", function (event) {
            packageOperation = event.target.value;
			if (packageOperation === "UPLOAD" || packageOperation === "UPLOAD_INSTALL") {
				$("input[name='./packageName']").attr("disabled", true);
				$("input[name='./packageGroup']").attr("disabled", true);
				$("input[name='./packageVersion']").attr("disabled", true);
				$("coral-fileupload[name='./inputPackage']").attr("disabled", false);
			} else if(packageOperation === "BUILD" || packageOperation === "INSTALL" || packageOperation === "DELETE"){
				$("input[name='./packageName']").attr("disabled", false);
				$("input[name='./packageGroup']").attr("disabled", false);
				$("input[name='./packageVersion']").attr("disabled", false);
				$("coral-fileupload[name='./inputPackage']").attr("disabled", true);
			}
        });

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

        function getFileByName(fieldName) {
            var fieldInput = $("input[name='" + fieldName + "']");
            if(null != fieldInput && null != fieldInput[0]){
                return fieldInput[0].files[0];
            }
        }

        $(document).off("click", ".package-initiator").on("click", ".package-initiator", function (event) {
            event.preventDefault();

            var packageName = getValueByName('./packageName', false),
                packageGroup = getValueByName('./packageGroup', false),
                packageVersion = getValueByName('./packageVersion', false),
                inputPackage = getFileByName('./inputPackage');

            if (!packageOperation) {
                return;
            }

            var formData = new FormData();
            if(null != inputPackage){
                formData.append("file", inputPackage, inputPackage.name);
            }
            formData.append("packageName", packageName);
            formData.append("packageGroup", packageGroup);
            formData.append("packageVersion", packageVersion);
            formData.append("packageOperation", packageOperation);

			$(".loading").html("PLEASE WAIT "+packageOperation+"ING PACKAGE");
			$(".modal").show();
			getStatus(true);

            $.ajax({
                url: "/bin/triggerPackageHandler",
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
                    ui.notify("Error", "Unable to process package operation", "error");
                }
            }).fail(function (data) {
				$(".modal").hide();
				getStatus(false);
                if (data && data.responseJSON && data.responseJSON.message){
                    ui.notify("Error", data.responseJSON.message, "error");
                }else{
                    ui.notify("Error", "Unable to process package operation", "error");
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
