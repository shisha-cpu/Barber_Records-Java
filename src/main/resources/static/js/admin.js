(() => {
    const adminCalendar = document.getElementById('adminCalendar');
    if (!adminCalendar) return;

    const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
        'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
    const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

    const state = {
        mode: 'calendar',
        month: new Date(),
        selectedDay: null,
        periodFrom: null,
        periodTo: null,
        bookings: [],
        availableRange: null
    };

    const modeButtons = document.querySelectorAll('[data-mode]');
    const calendarMode = document.getElementById('calendarMode');
    const periodMode = document.getElementById('periodMode');
    const monthTitle = document.getElementById('monthTitle');
    const detailPanel = document.getElementById('detailPanel');
    const detailTitle = document.getElementById('detailTitle');
    const detailBookings = document.getElementById('detailBookings');
    const availablePanel = document.getElementById('availablePanel');
    const availableServiceSelect = document.getElementById('availableServiceSelect');
    const availableSlotsResult = document.getElementById('availableSlotsResult');
    const shareActions = document.getElementById('shareActions');
    const periodFromInput = document.getElementById('periodFrom');
    const periodToInput = document.getElementById('periodTo');
    const periodBookings = document.getElementById('periodBookings');

    initServicesSelect();
    bindEvents();
    renderCalendarMode();

    function initServicesSelect() {
        const services = window.ADMIN_SERVICES || [];
        availableServiceSelect.innerHTML = services.map((s) =>
            `<option value="${s.id}">${s.name} · ${s.durationMinutes} мин · ${formatPrice(s.price)}</option>`
        ).join('');
        if (services.length) {
            availableServiceSelect.addEventListener('change', loadAvailableSlots);
        }
    }

    function bindEvents() {
        modeButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                modeButtons.forEach((b) => b.classList.remove('active'));
                btn.classList.add('active');
                state.mode = btn.dataset.mode;
                calendarMode.classList.toggle('hidden', state.mode !== 'calendar');
                periodMode.classList.toggle('hidden', state.mode !== 'period');
                hidePanels();
                if (state.mode === 'calendar') renderCalendarMode();
                if (state.mode === 'period') renderPeriodMode();
            });
        });

        document.getElementById('monthPrev').addEventListener('click', () => shiftMonth(-1));
        document.getElementById('monthNext').addEventListener('click', () => shiftMonth(1));
        document.getElementById('showAvailableBtn').addEventListener('click', openAvailablePanel);
        document.getElementById('periodShowBtn').addEventListener('click', renderPeriodMode);
        document.getElementById('sharePdfBtn').addEventListener('click', sharePdf);
        document.getElementById('downloadPdfBtn').addEventListener('click', downloadPdf);
    }

    async function renderCalendarMode() {
        const range = monthRange(state.month);
        monthTitle.textContent = `${MONTHS[state.month.getMonth()]} ${state.month.getFullYear()}`;
        adminCalendar.innerHTML = '<p class="hint">Загрузка...</p>';

        state.bookings = await fetchBookings(range.from, range.to);
        adminCalendar.innerHTML = '';
        adminCalendar.appendChild(buildMonthGrid(state.month, state.bookings));

        if (state.selectedDay) {
            showDayDetail(state.selectedDay);
        }
    }

    async function renderPeriodMode() {
        const from = periodFromInput.value;
        const to = periodToInput.value;
        if (!from || !to) {
            periodBookings.innerHTML = '<p class="hint">Выберите период</p>';
            return;
        }
        if (from > to) {
            periodBookings.innerHTML = '<p class="error">Дата начала позже даты окончания</p>';
            return;
        }

        state.periodFrom = from;
        state.periodTo = to;
        periodBookings.innerHTML = '<p class="hint">Загрузка...</p>';

        const bookings = await fetchBookings(from, to);
        state.bookings = bookings;

        const header = document.createElement('div');
        header.className = 'detail-panel-header';
        header.innerHTML = `
            <h3>Записи за ${formatDateRu(from)} — ${formatDateRu(to)}</h3>
            <button type="button" class="btn btn-secondary btn-sm" id="periodAvailableBtn">Доступные записи</button>
        `;
        periodBookings.innerHTML = '';
        periodBookings.appendChild(header);

        header.querySelector('#periodAvailableBtn').addEventListener('click', () => {
            state.availableRange = { from, to };
            openAvailablePanel();
        });

        const grouped = groupByDate(bookings);
        if (!bookings.length) {
            const empty = document.createElement('p');
            empty.className = 'empty';
            empty.textContent = 'Записей за период нет';
            periodBookings.appendChild(empty);
            return;
        }

        Object.keys(grouped).sort().forEach((date) => {
            const section = document.createElement('div');
            section.className = 'period-day-section';
            section.innerHTML = `<h4>${formatDateRu(date)}</h4>`;
            const list = document.createElement('div');
            list.className = 'booking-list';
            grouped[date].forEach((b) => list.appendChild(createBookingCard(b)));
            section.appendChild(list);
            periodBookings.appendChild(section);
        });
    }

    function buildMonthGrid(ref, bookings) {
        const year = ref.getFullYear();
        const month = ref.getMonth();
        const first = new Date(year, month, 1);
        const last = new Date(year, month + 1, 0);
        const todayIso = toIso(new Date());
        const grouped = groupByDate(bookings);

        const wrap = document.createElement('div');
        wrap.className = 'admin-month-calendar';

        const weekdays = document.createElement('div');
        weekdays.className = 'calendar-weekdays';
        WEEKDAYS.forEach((d) => {
            const cell = document.createElement('div');
            cell.textContent = d;
            weekdays.appendChild(cell);
        });
        wrap.appendChild(weekdays);

        const grid = document.createElement('div');
        grid.className = 'calendar-grid admin-calendar-grid';

        let offset = first.getDay() - 1;
        if (offset < 0) offset = 6;
        for (let i = 0; i < offset; i++) {
            grid.appendChild(document.createElement('div'));
        }

        for (let day = 1; day <= last.getDate(); day++) {
            const date = toIso(new Date(year, month, day));
            const dayBookings = grouped[date] || [];
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'calendar-day admin-calendar-day';
            if (date === state.selectedDay) btn.classList.add('selected');
            if (date === todayIso) btn.classList.add('today');

            btn.innerHTML = `
                <span class="day-number">${day}</span>
                ${dayBookings.length ? `<span class="day-count">${dayBookings.length}</span>` : ''}
            `;

            btn.addEventListener('click', () => {
                state.selectedDay = date;
                state.availableRange = { from: date, to: date };
                renderCalendarMode();
            });

            grid.appendChild(btn);
        }

        wrap.appendChild(grid);
        return wrap;
    }

    function showDayDetail(date) {
        const dayBookings = state.bookings.filter((b) => b.date === date);
        detailPanel.classList.remove('hidden');
        detailTitle.textContent = `Записи на ${formatDateRu(date)}`;
        detailBookings.innerHTML = '';

        if (!dayBookings.length) {
            detailBookings.innerHTML = '<p class="empty">Записей нет</p>';
        } else {
            dayBookings.forEach((b) => detailBookings.appendChild(createBookingCard(b)));
        }

        state.availableRange = { from: date, to: date };
    }

    function openAvailablePanel() {
        if (!state.availableRange) return;
        availablePanel.classList.remove('hidden');
        loadAvailableSlots();
    }

    async function loadAvailableSlots() {
        if (!state.availableRange || !availableServiceSelect.value) return;

        availableSlotsResult.innerHTML = '<p class="hint">Загрузка...</p>';
        shareActions.classList.add('hidden');

        const params = new URLSearchParams({
            serviceId: availableServiceSelect.value,
            from: state.availableRange.from,
            to: state.availableRange.to
        });

        const response = await fetch(`/admin/api/available-slots?${params}`);
        const data = await response.json();

        availableSlotsResult.innerHTML = '';
        if (!data.days || !data.days.length) {
            availableSlotsResult.innerHTML = '<p class="empty">Свободных окон нет</p>';
            return;
        }

        data.days.forEach((day) => {
            const block = document.createElement('div');
            block.className = 'available-day-block';
            block.innerHTML = `<h4>${formatDateRu(day.date)}</h4>`;
            const slots = document.createElement('div');
            slots.className = 'time-slots';
            day.times.forEach((time) => {
                const span = document.createElement('span');
                span.className = 'time-slot readonly';
                span.textContent = time;
                slots.appendChild(span);
            });
            block.appendChild(slots);
            availableSlotsResult.appendChild(block);
        });

        shareActions.classList.remove('hidden');
    }

    async function sharePdf() {
        const blob = await fetchPdfBlob();
        if (!blob) return;

        const file = new File([blob], 'available-slots.pdf', { type: 'application/pdf' });
        if (navigator.share && navigator.canShare && navigator.canShare({ files: [file] })) {
            try {
                await navigator.share({
                    files: [file],
                    title: 'Доступные записи',
                    text: 'Свободные окна для записи в Barber Records'
                });
                return;
            } catch (e) {
                if (e.name === 'AbortError') return;
            }
        }
        downloadPdf();
    }

    async function downloadPdf() {
        const blob = await fetchPdfBlob();
        if (!blob) return;

        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'available-slots.pdf';
        link.click();
        URL.revokeObjectURL(url);
    }

    async function fetchPdfBlob() {
        if (!state.availableRange || !availableServiceSelect.value) return null;
        const params = new URLSearchParams({
            serviceId: availableServiceSelect.value,
            from: state.availableRange.from,
            to: state.availableRange.to
        });
        const response = await fetch(`/admin/api/available-slots/pdf?${params}`);
        if (!response.ok) return null;
        return response.blob();
    }

    function createBookingCard(booking) {
        const card = document.createElement('div');
        card.className = 'booking-card';
        card.innerHTML = `
            <div>
                <strong>${booking.time} — ${booking.serviceName}</strong>
                <div class="booking-card-meta">${booking.clientName} · ${booking.clientPhone}</div>
                <div class="booking-card-meta">${booking.durationMinutes} мин · ${formatPrice(booking.price)}</div>
            </div>
            <form action="/admin/bookings/${booking.id}/delete" method="post">
                <input type="hidden" name="_csrf" value="${getCsrfToken()}">
                <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('Удалить запись?')">Удалить</button>
            </form>
        `;
        return card;
    }

    function hidePanels() {
        detailPanel.classList.add('hidden');
        availablePanel.classList.add('hidden');
        state.selectedDay = null;
    }

    function shiftMonth(dir) {
        state.month = new Date(state.month.getFullYear(), state.month.getMonth() + dir, 1);
        renderCalendarMode();
    }

    async function fetchBookings(from, to) {
        const params = new URLSearchParams({ from, to });
        const response = await fetch(`/admin/api/bookings?${params}`);
        return response.json();
    }

    function monthRange(ref) {
        const first = new Date(ref.getFullYear(), ref.getMonth(), 1);
        const last = new Date(ref.getFullYear(), ref.getMonth() + 1, 0);
        return { from: toIso(first), to: toIso(last) };
    }

    function groupByDate(bookings) {
        return bookings.reduce((acc, b) => {
            if (!acc[b.date]) acc[b.date] = [];
            acc[b.date].push(b);
            return acc;
        }, {});
    }

    function getCsrfToken() {
        const input = document.getElementById('csrfToken');
        return input ? input.value : '';
    }

    function toIso(date) {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }

    function formatDateRu(iso) {
        const [y, m, d] = iso.split('-').map(Number);
        return new Date(y, m - 1, d).toLocaleDateString('ru-RU', {
            weekday: 'short', day: 'numeric', month: 'long'
        });
    }

    function formatPrice(value) {
        return `${Number(value).toLocaleString('ru-RU')} ₽`;
    }

    const today = toIso(new Date());
    periodFromInput.value = today;
    periodToInput.value = toIso(new Date(Date.now() + 6 * 86400000));
})();
