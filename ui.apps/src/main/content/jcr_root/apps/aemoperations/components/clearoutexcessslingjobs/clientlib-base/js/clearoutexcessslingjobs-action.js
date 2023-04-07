var Coral = window.Coral || {},
    Granite = window.Granite || {};

(function(window, document, $, Coral) {
    "use strict";
    $(document).on("foundation-contentloaded", function(e) {

        var SITE_PATH = "/conf/aemoperations/settings/tools/clearoutexcessslingjobs-initiator.html",
            ui = $(window).adaptTo("foundation-ui");

        if (window.location.href.indexOf(SITE_PATH) < 0) {
            return;
        }

        $(document).off("change", ".close").on("change", ".close", function(event) {
            $(".modal").hide();
        });

        $(document).on("keyup", "input[name='./table-search']", function() {
            var value = $(this).val().toLowerCase();
            $("#package-table-body tr").filter(function() {
                $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1)
            });
        });

        $(document).off("click", ".delete-job").on("click", ".delete-job", function(event) {
            event.preventDefault();
            var eventOperation = event.target.parentElement.parentElement;
            var formData = new FormData();
            formData.append("jobTopic", eventOperation.children[0].textContent);

            //Add loading Message here
            $(".loading").html("in progress");
            $(".modal").show();

            $.ajax({
                // add the servlet path
                url: "/bin/triggerclearoutexcessslingjobs",
                method: "POST",
                async: true,
                cache: false,
                contentType: false,
                processData: false,
                data: formData
            }).done(function(data) {
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
                } else {
                    //add error message
                    ui.notify("Error", "Unable to process the request", "error");
                }
            }).fail(function(data) {
                $(".modal").hide();
                if (data && data.responseJSON && data.responseJSON.message) {
                    ui.notify("Error", data.responseJSON.message, "error");
                } else {
                    //add error message
                    ui.notify("Error", "Unable to process request", "error");
                }
            });
        });
    });
})(window, document, $, Coral);
