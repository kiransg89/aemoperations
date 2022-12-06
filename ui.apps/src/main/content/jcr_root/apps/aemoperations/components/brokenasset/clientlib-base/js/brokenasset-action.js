var Coral = window.Coral || {},
   Granite = window.Granite || {};

(function (window, document, $, Coral) {
   "use strict";

   var called = false;
   $(document).on("foundation-contentloaded", function (e) {
      if (called == false) {
         getStatus();
         called = true;
      }

      var SITE_PATH = "/conf/aemoperations/settings/tools/brokenasset-initiator.html",
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

      $(document).off("click", ".activation-initiator").on("click", ".activation-initiator", function (event) {
         event.preventDefault();
         // reading text fields and files
         var selectReport = getValueByName('./selectReport', false),
            schedulerExpression = getValueByName('./schedulerExpression', false);

         var formData = new FormData();
         formData.append("selectReport", selectReport);
         formData.append("schedulerExpression", schedulerExpression);

         //Add loading Message here
         $(".loading").html("in progress");
         $(".modal").show();

         $.ajax({
            // add the servlet path
            url: "/bin/triggerbrokenasset",
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
               emptyResults();
               getStatus();
            } else {
               //add error message
               ui.notify("Error", "Unable to process the request", "error");
            }
         }).fail(function (data) {
            $(".modal").hide();
            if (data && data.responseJSON && data.responseJSON.message) {
               ui.notify("Error", data.responseJSON.message, "error");
            } else {
               //add error message
               ui.notify("Error", "Unable to process request", "error");
            }
         });
      });

      $(document).off("click", ".activation-deactivator").on("click", ".activation-deactivator", function (event) {
         var jobPath = event.target.parentElement.value;

         $.ajax({
            // add the servlet path
            url: "/bin/triggerbrokenasset?jobPath=" + jobPath,
            method: "DELETE",
            async: true,
            cache: false,
            contentType: false,
            processData: false
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
               emptyResults();
               getStatus();
            } else {
               //add error message
               ui.notify("Error", "Unable to unschedule Asset Reference Search Job", "error");
            }
         }).fail(function (data) {
            $(".modal").hide();
            if (data && data.responseJSON && data.responseJSON.message) {
               ui.notify("Error", data.responseJSON.message, "error");
            } else {
               //add error message
               ui.notify("Error", "Unable to unschedule Asset Reference Search Job", "error");
            }
         });
      });

      function emptyResults() {
         $("#table-body").empty();
         $('#table-head').empty();
      }

      function getStatus() {
         var timeout = getValueByName('./timeout', false);
         if (timeout) {
            timeout = timeout * 1000;
         } else {
            timeout = 10 * 1000;
         }

         $.ajax({
            // add the servlet path
            url: "/bin/triggerbrokenasset",
            method: "GET",
            async: true,
            cache: false,
            contentType: false,
            processData: false
         }).done(function (data) {
            if (data) {
               data = JSON.parse(data);
               $('#table-head').append(`<tr>
                                    <th>Report Name</th>
                                    <th>Status</th>                                    
                                    <th>Current Row</th>
                                    <th>CPU Usage</th>
                                    <th>Memory (Heap) Usage</th>
                                    <th>Scheduled Expression</th>
                                    <th>Action</th> </tr>`);
               for (var i = 0; i < data.length; ++i) {
                  if (data[i].jobStatus === "running" || data[i].jobStatus === "starting") {
                     runningResult(data[i]);
                     setTimeout(() => {
                        emptyResults();
                        getStatus();
                     }, timeout);
                  } else if (data[i].jobStatus === "unscheduled") {
                     unscheduledResult(data[i]);
                  } else {
                     completedResult(data[i]);
                  }
               }
            } else {
               $(".modal").hide();
               ui.notify("Error", "Unable to schedule Asset Reference Search Job", "error");
            }
         }).fail(function (data) {
            $(".modal").hide();
            if (data && data.responseJSON && data.responseJSON.message) {
               ui.notify("Error", data.responseJSON.message, "error");
            } else {
               //add error message
               ui.notify("Error", "Unable to process request", "error");
            }
         });
      }

      function runningResult(data) {
         $("#table-body").append(`<tr class="search-row">	             
						<td><a href="${data.csvPath}">${data.reportName}</a></td>																
						<td><coral-icon alt="" class="coral3-Icon coral3-Icon--sizeS coral3-Icon--rotateRight" icon="rotateRight" size="S"></coral-icon> ${data.jobStatus}</td>						
						<td>${data.current}</td>
						<td>${data.cpu}/${data.maxCpu}</td>
			            <td>${data.mem}/${data.maxMem}</td>		
						<td>${data.scheduledExpression}</td>
						<td>            
						<button class="coral3-Button coral3-Button--primary activation-deactivator" icon="rotateLeft" iconsize="M"
               			is="coral-button" value=${data.reportPath}
		               size="M" variant="primary">
        		       		<coral-icon alt="" class="coral3-Icon coral3-Icon--sizeS coral3-Icon--publishRemove" icon="publishRemove"
	                		  size="S"></coral-icon>
			               <coral-button-label>
            			      Unschedule Report
			               </coral-button-label>
            			</button>
			            </td>		 	             		                	                	                
		           		 </tr>`);
      }

      function completedResult(data) {
         $("#table-body").append(`<tr class="search-row">	             
						<td><a href="${data.csvPath}">${data.reportName}</a></td>																
						<td><coral-icon alt="" class="coral3-Icon coral3-Icon--sizeS coral3-Icon--checkCircle" icon="checkCircle" size="S"></coral-icon> ${data.jobStatus}</td>						
						<td>${data.current}</td>
						<td>${data.cpu}/${data.maxCpu}</td>
			            <td>${data.mem}/${data.maxMem}</td>
						<td>${data.scheduledExpression}</td>
						<td>            
						<button class="coral3-Button coral3-Button--primary activation-deactivator" icon="publishRemove" iconsize="M"
               			is="coral-button" value=${data.reportPath}
		               size="M" variant="primary">
        		       		<coral-icon alt="" class="coral3-Icon coral3-Icon--sizeS coral3-Icon--publishRemove" icon="publishRemove"
	                		  size="S"></coral-icon>
			               <coral-button-label>
            			      Unschedule Report
			               </coral-button-label>
            			</button>
			            </td>			 	             		                	                	                
		           		 </tr>`);
      }

      function unscheduledResult(data) {
         $("#table-body").append(`<tr class="search-row">	             
						<td><a href="${data.csvPath}">${data.reportName}</a></td>																		
						<td><coral-icon alt="" class="coral-Form-fielderror coral3-Icon coral3-Icon--sizeS coral3-Icon--stopCircle" icon="stopCircle" size="S"></coral-icon> ${data.jobStatus}</td>						
						<td>${data.current}</td>
						<td>${data.cpu}/${data.maxCpu}</td>
			            <td>${data.mem}/${data.maxMem}</td>
						<td>${data.scheduledExpression}</td>
						<td>            
						<button class="coral3-Button coral3-Button--primary activation-deactivator" icon="publishRemove" iconsize="M"
               			is="coral-button" value=${data.reportPath}
		               size="M" variant="primary">
        		       		<coral-icon alt="" class="coral3-Icon coral3-Icon--sizeS coral3-Icon--publishRemove" icon="publishRemove"
	                		  size="S"></coral-icon>
			               <coral-button-label>
            			      Unschedule Report
			               </coral-button-label>
            			</button>
			            </td>				 	             		                	                	                
		           		 </tr>`);
      }
   });
})(window, document, $, Coral);