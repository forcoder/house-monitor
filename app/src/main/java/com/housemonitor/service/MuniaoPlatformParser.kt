package com.housemonitor.service

class MuniaoPlatformParser : PlatformParser {
    override val platformId: String = "muniao"
    override val platformName: String = "木鸟民宿"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("muniao.com") || url.contains("muniao")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
            (function() {
                try {
                    var results = [];
                    var cells = document.querySelectorAll('.mu-calendar .day-item, .calendar-date, .day-cell, [data-date], [data-calendar-date]');
                    var unavailableClasses = ['mu-disabled', 'sold', 'disabled', 'unavailable', 'booked', 'full', 'closed'];

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
                        if (!isUnavailable && cell.getAttribute('aria-disabled') === 'true') {
                            isUnavailable = true;
                        }

                        if (isUnavailable) {
                            var dateStr = cell.getAttribute('data-date') || cell.getAttribute('data-calendar-date') || cell.getAttribute('data-day') || '';
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
