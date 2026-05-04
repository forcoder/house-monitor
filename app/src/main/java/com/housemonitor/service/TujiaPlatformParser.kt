package com.housemonitor.service

class TujiaPlatformParser : PlatformParser {
    override val platformId: String = "tujia"
    override val platformName: String = "途家"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("tujia.com") || url.contains("tujia")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
            (function() {
                try {
                    var results = [];
                    var cells = document.querySelectorAll('.calendar-panel .day-item, .day-cell, .date-item, [data-date]');
                    var unavailableClasses = ['disabled', 'off', 'sold-out', 'unavailable', 'booked', 'full', 'closed'];

                    for (var i = 0; i < cells.length; i++) {
                        var cell = cells[i];
                        var className = cell.className || '';
                        var isUnavailable = false;

                        for (var j = 0; j < unavailableClasses.length; j++) {
                            if (className.indexOf(unavailableClasses[j]) !== -1) {
                                isUnavailable = true;
                                break;
                            }
                        }

                        if (!isUnavailable && cell.getAttribute('data-available') === 'false') {
                            isUnavailable = true;
                        }
                        if (!isUnavailable && cell.getAttribute('data-status') === 'unavailable') {
                            isUnavailable = true;
                        }

                        if (isUnavailable) {
                            var dateStr = cell.getAttribute('data-date') || cell.getAttribute('data-day') || '';
                            if (dateStr) {
                                results.push(dateStr);
                            }
                        }
                    }

                    return JSON.stringify(results);
                } catch (error) {
                    return JSON.stringify([]);
                }
            })();
        """.trimIndent()
    }
}
