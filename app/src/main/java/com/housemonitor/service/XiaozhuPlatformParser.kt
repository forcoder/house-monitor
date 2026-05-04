package com.housemonitor.service

class XiaozhuPlatformParser : PlatformParser {
    override val platformId: String = "xiaozhu"
    override val platformName: String = "小猪民宿"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("xiaozhu.com") || url.contains("xiaozhu")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
            (function() {
                var results = [];
                var cells = document.querySelectorAll('.room-calendar .day, .calendar-day, .date-cell, [data-date]');
                var unavailableClasses = ['disabled', 'not-available', 'unavailable', 'booked', 'occupied', 'full'];

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

                    if (isUnavailable) {
                        var dateStr = cell.getAttribute('data-date') || cell.getAttribute('data-day') || '';
                        if (dateStr) {
                            results.push(dateStr);
                        }
                    }
                }

                return JSON.stringify(results);
            })();
        """.trimIndent()
    }
}
