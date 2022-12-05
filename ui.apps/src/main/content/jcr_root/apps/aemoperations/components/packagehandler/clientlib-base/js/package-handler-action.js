var Coral = window.Coral || {},
    Granite = window.Granite || {};

(function (window, document, $, Coral) {
    "use strict";
    var packageOperation = "UPLOAD";
    var called = false;
    $(document).on("foundation-contentloaded", function (e) {

	if (called == false) {
         getPackageList();
         called = true;
      }

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

        function getFileByName(fieldName) {
            var fieldInput = $("input[name='" + fieldName + "']");
            if(null != fieldInput && null != fieldInput[0]){
                return fieldInput[0].files[0];
            }
        }

        $(document).off("click", ".package-initiator").on("click", ".package-initiator", function (event) {
            event.preventDefault();

            var inputPackage = getFileByName('./inputPackage');

            if (!packageOperation) {
                return;
            }

            var formData = new FormData();
            if(null != inputPackage){
                formData.append("file", inputPackage, inputPackage.name);
            }

            formData.append("packageOperation", packageOperation);
            callOperation(formData);
            emptyPackageResults();
            getPackageList();
        });

        function getPackageList() {
         $.ajax({
            // add the servlet path
            url: "/bin/triggerPackageHandler",
            method: "GET",
            async: true,
            cache: false,
            contentType: false,
            processData: false
         }).done(function (data) {
            if (data) {
               data = JSON.parse(data);
               $('#package-table-head').append(`<tr>
                                    <th>Package Name</th>
                                    <th>Group</th>
                                    <th>Version</th>
                                    <th>Build</th>
                                    <th>Install</th>
                                    <th>Delete</th>
                                    <th>Download</th> </tr>`);
               for (var i = 0; i < data.length; ++i) {
					$("#package-table-body").append(`<tr class="search-row">
						<td class="packageName">${data[i].packageName}</td>
						<td class="packageGroup">${data[i].packageGroup}</td>
						<td class="packageVersion">${data[i].packageVersion}</td>
						<td><coral-icon style="cursor: pointer;color:#f5b95a" alt="build" class="package-build coral3-Icon coral3-Icon--sizeS coral3-Icon--box" icon="box" size="M"></coral-icon></td>
						<td><coral-icon style="cursor: pointer;color:green" alt="install" class="package-install coral3-Icon coral3-Icon--sizeS coral3-Icon--boxImport" icon="boxImport" size="M"></coral-icon></td>
						<td><coral-icon style="cursor: pointer;color:#c83728" alt="delete" class="package-delete coral3-Icon coral3-Icon--sizeS coral3-Icon--delete" icon="delete" size="M"></coral-icon></td>
						<td><a href="${data[i].packagePath}""><coral-icon alt="download" style="color:#0074D9" class="coral3-Icon coral3-Icon--sizeS coral3-Icon--download" icon="download" size="M"></coral-icon> </a></td>
		           		</tr>`);
               }
            } else {
               $(".modal").hide();
               ui.notify("Error", "Unable to schedule Asset Reference Search Job", "error");
            }
         }).fail(function () {
            $(".modal").hide();
            ui.notify("Error", "Unable to retrive packages", "error");
         });
      }

	  $(document).off("click", ".package-build").on("click", ".package-build", function (event) {
        event.preventDefault();
		var eventOperation = event.target.parentElement.parentElement;
        var formData = new FormData();
            formData.append("packageName", eventOperation.children[0].textContent);
            formData.append("packageGroup", eventOperation.children[1].textContent);
            formData.append("packageVersion", eventOperation.children[2].textContent);
            formData.append("packageOperation", "BUILD");
            callOperation(formData);
            emptyPackageResults();
            getPackageList();
	  });

      $(document).off("click", ".package-install").on("click", ".package-install", function (event) {
        event.preventDefault();
		var eventOperation = event.target.parentElement.parentElement;
        var formData = new FormData();
            formData.append("packageName", eventOperation.children[0].textContent);
            formData.append("packageGroup", eventOperation.children[1].textContent);
            formData.append("packageVersion", eventOperation.children[2].textContent);
            formData.append("packageOperation", "INSTALL");
            callOperation(formData);
            emptyPackageResults();
            getPackageList();
	  });

      $(document).off("click", ".package-delete").on("click", ".package-delete", function (event) {
        event.preventDefault();
		var eventOperation = event.target.parentElement.parentElement;
        var formData = new FormData();
            formData.append("packageName", eventOperation.children[0].textContent);
            formData.append("packageGroup", eventOperation.children[1].textContent);
            formData.append("packageVersion", eventOperation.children[2].textContent);
            formData.append("packageOperation", "DELETE");
            callOperation(formData);
            emptyPackageResults();
            getPackageList();
	  });

      function callOperation(formData) {
			$(".loading").html("PLEASE WAIT "+formData.get("packageOperation")+"ING PACKAGE");
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
      }

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

     function emptyPackageResults() {
      $("#package-table-head").empty();
      $("#package-table-body").empty();
     }
    });
})(window, document, $, Coral);
