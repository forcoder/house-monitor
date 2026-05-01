package com.housemonitor.service

class MeituanPlatformParser : PlatformParser {
    override val platformId: String = "meituan"
    override val platformName: String = "美团民宿"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("meituan.com") || url.contains("meituan")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
(function() {
    try {
        var calendarElements = [];
        var selectors = [
            '.calendar-cell', '.calendar-item', '[data-calendar]',
            '.date-cell', '.day-cell', '[class*="calendar"]', '[class*="date"]'
        ];
        selectors.forEach(function(selector) {
            var elements = document.querySelectorAll(selector);
            if (elements.length > 0) {
                calendarElements = Array.from(elements);
                return;
            }
        });
        if (calendarElements.length === 0) {
            var allElements = document.querySelectorAll('*');
            calendarElements = Array.from(allElements).filter(function(el) {
                var text = el.textContent || '';
                return /\d{4}-\d{2}-\d{2}/.test(text) ||
                       /\d{1,2}[月日]/.test(text) ||
                       el.classList.toString().includes('date') ||
                       el.classList.toString().includes('calendar');
            });
        }
        var unavailableDates = [];
        var today = new Date();
        calendarElements.forEach(function(element) {
            var isUnavailable = false;
            var dateStr = '';
            if (element.classList.contains('unavailable') ||
                element.classList.contains('disabled') ||
                element.classList.contains('occupied') ||
                element.classList.contains('booked') ||
                element.classList.contains('full')) {
                isUnavailable = true;
            }
            if (element.getAttribute('data-available') === 'false' ||
                element.getAttribute('data-status') === 'unavailable' ||
                element.getAttribute('disabled') !== null) {
                isUnavailable = true;
            }
            var style = window.getComputedStyle(element);
            if (style.color === 'rgb(128, 128, 128)' ||
                style.color === 'rgb(153, 153, 153)' ||
                style.opacity === '0.5') {
                isUnavailable = true;
            }
            dateStr = element.getAttribute('data-date') ||
                     element.getAttribute('data-day') ||
                     element.textContent.trim();
            if (dateStr && isUnavailable) {
                var parsedDate = parseDate(dateStr, today);
                if (parsedDate) unavailableDates.push(parsedDate);
            }
        });
        return JSON.stringify(unavailableDates);
    } catch (error) {
        return JSON.stringify([]);
    }
})();
function parseDate(dateStr, today) {
    try {
        var patterns = [
            /(\d{4})-(\d{1,2})-(\d{1,2})/,
            /(\d{1,2})[月\/\\-](\d{1,2})[日]?/
        ];
        for (var i = 0; i < patterns.length; i++) {
            var match = dateStr.match(patterns[i]);
            if (match) {
                if (i === 0) return match[0];
                var month = parseInt(match[1]);
                var day = parseInt(match[2]);
                var year = today.getFullYear();
                if (month < today.getMonth() + 1) year++;
                return year + '-' + month.toString().padStart(2, '0') + '-' + day.toString().padStart(2, '0');
            }
        }
        var date = new Date(dateStr);
        if (!isNaN(date.getTime())) {
            return date.getFullYear() + '-' + (date.getMonth()+1).toString().padStart(2,'0') + '-' + date.getDate().toString().padStart(2,'0');
        }
        return null;
    } catch(e) { return null; }
}
""".trimIndent()
    }
}
