var Coral = window.Coral || {},
   Granite = window.Granite || {};

(function (window, document, $, Coral) {
   "use strict";
   $(document).on("foundation-contentloaded", function (e) {

      var SITE_PATH = "/conf/aemoperations/settings/tools/listreplication-initiator.html",
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

      function getFileByName(fieldName) {
         var fieldInput = $("input[name='" + fieldName + "']");
         if (null != fieldInput && null != fieldInput[0]) {
            return fieldInput[0].files[0];
         }
      }

      $(document).off("click", ".activation-initiator").on("click", ".activation-initiator", function (event) {
         event.preventDefault();

         var queueMethod = getValueByName('./queueMethod', false),
            agents = getValueByName('./agents', false),
            reAction = getValueByName('./reAction', false),
            pathsList = getValueByNameArea('./pathsList', true),
            repPathExcel = getFileByName('./repPathExcel');

         var formData = new FormData();
         if (null != repPathExcel) {
            formData.append("file", repPathExcel, repPathExcel.name);
         }
         formData.append("queueMethod", queueMethod);
         formData.append("agents", agents);
         formData.append("reAction", reAction);
         formData.append("pathsList", pathsList);

         $(".loading").html("Replication in progress");
         $(".modal").show();
         getStatus(true);

         $.ajax({
            url: "/bin/triggerListActivation",
            method: "POST",
            async: true,
            cache: false,
            contentType: false,
            processData: false,
            data: formData
         }).done(function (data) {
            if (data && data.message) {
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
            } else {
               ui.notify("Error", "Unable to process List Activation", "error");
            }
         }).fail(function (data) {
            $(".modal").hide();
            getStatus(false);
            if (data && data.responseJSON && data.responseJSON.message) {
               ui.notify("Error", data.responseJSON.message, "error");
            } else {
               ui.notify("Error", "Unable to process List Activation", "error");
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
   });

   function emptyResults() {
      $("#table-body").empty();
   }
})(window, document, $, Coral);