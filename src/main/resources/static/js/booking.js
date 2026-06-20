(() => {
    const MONTHS = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
        'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
    const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

    const state = {
        step: 0,
        service: null,
        date: null,
        time: null,
        weekStart: startOfDay(new Date()),
        closedDates: new Set()
    };

    const welcome = document.getElementById('stepWelcome');
    const quiz = document.getElementById('quizFlow');
    const startBtn = document.getElementById('startBookingBtn');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const confirmBtn = document.getElementById('confirmBtn');
    const homeBtn = document.getElementById('homeBtn');
    const serviceSearch = document.getElementById('serviceSearch');
    const serviceList = document.getElementById('serviceList');
    const noServicesHint = document.getElementById('noServicesHint');
    const weekPickerEl = document.getElementById('weekPicker');
    const selectedDateInput = document.getElementById('selectedDate');
    const timeSlots = document.getElementById('timeSlots');
    const timeSlotsArea = document.querySelector('.time-slots-area');
    const noSlotsHint = document.getElementById('noSlotsHint');
    const formError = document.getElementById('formError');
    const formSuccess = document.getElementById('formSuccess');

    let slotsRequestId = 0;

    startBtn.addEventListener('click', () => {
        welcome.classList.add('hidden');
        quiz.classList.remove('hidden');
        goToStep(1);
    });

    prevBtn.addEventListener('click', () => {
        if (state.step === 1) {
            quiz.classList.add('hidden');
            welcome.classList.remove('hidden');
            state.step = 0;
            return;
        }
        goToStep(state.step - 1);
    });

    nextBtn.addEventListener('click', async () => {
        clearMessages();
        if (!validateStep(state.step)) return;

        if (state.step === 3) {
            fillPreview();
        }
        goToStep(state.step + 1);
    });

    confirmBtn.addEventListener('click', submitBooking);
    homeBtn.addEventListener('click', goHome);

    serviceSearch.addEventListener('input', filterServices);

    document.querySelectorAll('.service-item').forEach((item) => {
        item.addEventListener('click', () => selectService(item));
    });

    function goToStep(step) {
        state.step = step;
        document.querySelectorAll('.progress-step').forEach((el) => {
            el.classList.toggle('active', Number(el.dataset.step) === step);
            el.classList.toggle('done', Number(el.dataset.step) < step);
        });
        document.querySelectorAll('[data-panel]').forEach((el) => {
            el.classList.toggle('hidden', Number(el.dataset.panel) !== step);
        });

        prevBtn.classList.toggle('hidden', step === 0);
        nextBtn.classList.toggle('hidden', step === 4);
        confirmBtn.classList.toggle('hidden', step !== 4);

        if (step === 2) {
            void initDateTimeStep();
        }
    }

    async function initDateTimeStep() {
        if (!state.date || isBeforeDay(parseIsoDate(state.date), startOfDay(new Date()))) {
            state.date = toIsoDate(startOfDay(new Date()));
            state.weekStart = startOfDay(new Date());
        }
        await renderWeekPicker();
        if (state.closedDates.has(state.date)) {
            const openCell = weekPickerEl.querySelector('.week-day-cell:not(.closed)');
            if (openCell) {
                state.date = openCell.dataset.date;
                selectedDateInput.value = state.date;
                updateWeekSelection();
            }
        }
        await loadTimeSlots();
    }

    async function fetchClosedDays(fromIso, toIso) {
        const params = new URLSearchParams({ from: fromIso, to: toIso });
        const response = await fetch(`/api/closed-days?${params}`);
        if (!response.ok) return new Set();
        return new Set(await response.json());
    }

    function selectService(item) {
        document.querySelectorAll('.service-item').forEach((el) => el.classList.remove('selected'));
        item.classList.add('selected');
        state.service = {
            id: item.dataset.id,
            name: item.dataset.name,
            duration: item.dataset.duration,
            price: item.dataset.price
        };
    }

    function filterServices() {
        const query = serviceSearch.value.trim().toLowerCase();
        let visible = 0;
        document.querySelectorAll('.service-item').forEach((item) => {
            const match = item.dataset.name.toLowerCase().includes(query);
            item.classList.toggle('hidden', !match);
            if (match) visible++;
        });
        noServicesHint.classList.toggle('hidden', visible > 0);
    }

    async function renderWeekPicker() {
        const today = startOfDay(new Date());
        const weekDays = [];
        for (let i = 0; i < 7; i++) {
            const date = new Date(state.weekStart);
            date.setDate(state.weekStart.getDate() + i);
            weekDays.push(date);
        }

        const weekFrom = toIsoDate(weekDays[0]);
        const weekTo = toIsoDate(weekDays[6]);
        state.closedDates = await fetchClosedDays(weekFrom, weekTo);

        const first = weekDays[0];
        const last = weekDays[6];
        const rangeLabel = first.getMonth() === last.getMonth()
            ? `${first.getDate()} — ${last.getDate()} ${MONTHS[last.getMonth()]}`
            : `${first.getDate()} ${MONTHS[first.getMonth()]} — ${last.getDate()} ${MONTHS[last.getMonth()]}`;

        const canGoPrev = state.weekStart > today;

        weekPickerEl.innerHTML = '';

        const header = document.createElement('div');
        header.className = 'week-picker-header';
        header.innerHTML = `
            <button type="button" class="week-nav${canGoPrev ? '' : ' hidden'}" id="prevWeek" aria-label="Предыдущая неделя">&larr;</button>
            <span class="week-range">${rangeLabel}</span>
            <button type="button" class="week-nav" id="nextWeek" aria-label="Следующая неделя">&rarr;</button>
        `;
        weekPickerEl.appendChild(header);

        const weekdaysRow = document.createElement('div');
        weekdaysRow.className = 'week-picker-days';
        weekDays.forEach((date) => {
            const iso = toIsoDate(date);
            const weekdayIndex = (date.getDay() + 6) % 7;
            const isWeekend = weekdayIndex >= 5;
            const isSelected = state.date === iso;

            const isClosed = state.closedDates.has(iso);

            const cell = document.createElement('button');
            cell.type = 'button';
            cell.className = 'week-day-cell';
            cell.dataset.date = iso;
            if (isWeekend) cell.classList.add('weekend');
            if (isSelected) cell.classList.add('selected');
            if (isClosed) {
                cell.classList.add('closed');
                cell.disabled = true;
            }
            cell.innerHTML = `
                <span class="week-day-name">${WEEKDAYS[weekdayIndex]}</span>
                <span class="week-day-num">${date.getDate()}</span>
            `;
            cell.addEventListener('click', async () => {
                if (isClosed || state.date === iso) return;
                state.date = iso;
                selectedDateInput.value = iso;
                state.time = null;
                updateWeekSelection();
                await loadTimeSlots();
            });
            weekdaysRow.appendChild(cell);
        });
        weekPickerEl.appendChild(weekdaysRow);

        const prevBtn = document.getElementById('prevWeek');
        const nextBtn = document.getElementById('nextWeek');
        if (prevBtn) {
            prevBtn.addEventListener('click', async () => {
                const nextStart = new Date(state.weekStart);
                nextStart.setDate(nextStart.getDate() - 7);
                if (nextStart < today) return;
                state.weekStart = nextStart;
                if (!state.date || !isDateInWeek(parseIsoDate(state.date), state.weekStart)) {
                    state.date = toIsoDate(state.weekStart);
                    selectedDateInput.value = state.date;
                }
                state.time = null;
                await renderWeekPicker();
                await loadTimeSlots();
            });
        }
        nextBtn.addEventListener('click', async () => {
            const nextStart = new Date(state.weekStart);
            nextStart.setDate(nextStart.getDate() + 7);
            state.weekStart = nextStart;
            if (!state.date || !isDateInWeek(parseIsoDate(state.date), state.weekStart)) {
                state.date = toIsoDate(state.weekStart);
                selectedDateInput.value = state.date;
            }
            state.time = null;
            await renderWeekPicker();
            await loadTimeSlots();
        });

        selectedDateInput.value = state.date || '';
    }

    function updateWeekSelection() {
        weekPickerEl.querySelectorAll('.week-day-cell').forEach((cell) => {
            cell.classList.toggle('selected', cell.dataset.date === state.date);
        });
    }

    function renderSlotButtons(times) {
        const fragment = document.createDocumentFragment();
        times.forEach((time) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'time-slot';
            btn.textContent = time;
            btn.addEventListener('click', () => {
                document.querySelectorAll('.time-slot').forEach((s) => s.classList.remove('selected'));
                btn.classList.add('selected');
                state.time = time;
            });
            fragment.appendChild(btn);
        });
        return fragment;
    }

    async function loadTimeSlots() {
        if (!state.service || !state.date) return;

        const requestId = ++slotsRequestId;
        timeSlotsArea.classList.add('loading');
        state.time = null;

        if (state.closedDates.has(state.date)) {
            timeSlots.replaceChildren();
            noSlotsHint.textContent = 'В этот день запись недоступна';
            noSlotsHint.classList.remove('hidden');
            timeSlotsArea.classList.remove('loading');
            return;
        }

        try {
            const params = new URLSearchParams({
                serviceId: state.service.id,
                date: state.date
            });
            const response = await fetch(`/api/times?${params}`);
            const times = await response.json();

            if (requestId !== slotsRequestId) return;

            noSlotsHint.textContent = 'На эту дату нет свободного времени';
            timeSlots.replaceChildren();
            noSlotsHint.classList.add('hidden');

            if (!times.length) {
                noSlotsHint.classList.remove('hidden');
                return;
            }

            timeSlots.appendChild(renderSlotButtons(times));
        } catch (e) {
            if (requestId !== slotsRequestId) return;
            timeSlots.replaceChildren();
            noSlotsHint.classList.remove('hidden');
            noSlotsHint.textContent = 'Не удалось загрузить время. Попробуйте ещё раз.';
        } finally {
            if (requestId === slotsRequestId) {
                timeSlotsArea.classList.remove('loading');
            }
        }
    }

    function fillPreview() {
        document.getElementById('previewService').textContent = state.service.name;
        document.getElementById('previewDuration').textContent = `${state.service.duration} мин`;
        document.getElementById('previewPrice').textContent = formatPrice(state.service.price);
        document.getElementById('previewDate').textContent = formatDateLabel(state.date);
        document.getElementById('previewTime').textContent = state.time;
        document.getElementById('previewName').textContent = document.getElementById('clientName').value.trim();
        document.getElementById('previewPhone').textContent = document.getElementById('clientPhone').value.trim();
    }

    async function submitBooking() {
        clearMessages();
        const payload = {
            serviceId: Number(state.service.id),
            date: state.date,
            time: state.time,
            clientName: document.getElementById('clientName').value.trim(),
            clientPhone: document.getElementById('clientPhone').value.trim(),
            website: document.getElementById('website').value.trim()
        };

        try {
            const response = await fetch('/api/bookings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (!response.ok) {
                showError(data.error || 'Не удалось создать запись');
                return;
            }
            formSuccess.textContent = data.message || 'Запись успешно создана';
            formSuccess.classList.remove('hidden');
            confirmBtn.disabled = true;
            nextBtn.disabled = true;
            prevBtn.disabled = true;
            homeBtn.classList.remove('hidden');
        } catch (e) {
            showError('Ошибка сети. Попробуйте позже.');
        }
    }

    function goHome() {
        state.step = 0;
        state.service = null;
        state.date = null;
        state.time = null;
        state.weekStart = startOfDay(new Date());
        state.closedDates = new Set();
        document.getElementById('clientName').value = '';
        document.getElementById('clientPhone').value = '';
        document.getElementById('website').value = '';
        document.querySelectorAll('.service-item').forEach((el) => el.classList.remove('selected'));
        clearMessages();
        confirmBtn.disabled = false;
        nextBtn.disabled = false;
        prevBtn.disabled = false;
        homeBtn.classList.add('hidden');
        quiz.classList.add('hidden');
        welcome.classList.remove('hidden');
    }

    function validateStep(step) {
        if (step === 1 && !state.service) {
            showError('Выберите услугу');
            return false;
        }
        if (step === 2) {
            if (!state.date) {
                showError('Выберите дату');
                return false;
            }
            if (!state.time) {
                showError('Выберите время');
                return false;
            }
        }
        if (step === 3) {
            const name = document.getElementById('clientName').value.trim();
            const phone = document.getElementById('clientPhone').value.trim();
            if (!name) {
                showError('Укажите ФИО');
                return false;
            }
            if (!phone) {
                showError('Укажите телефон');
                return false;
            }
        }
        return true;
    }

    function showError(message) {
        formError.textContent = message;
        formError.classList.remove('hidden');
        formSuccess.classList.add('hidden');
    }

    function clearMessages() {
        formError.classList.add('hidden');
        formSuccess.classList.add('hidden');
    }

    function formatPrice(value) {
        return `от ${Number(value).toLocaleString('ru-RU')} ₽`;
    }

    function formatDateLabel(iso) {
        const [y, m, d] = iso.split('-').map(Number);
        return `${String(d).padStart(2, '0')}.${String(m).padStart(2, '0')}.${y}`;
    }

    function toIsoDate(date) {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }

    function parseIsoDate(iso) {
        const [y, m, d] = iso.split('-').map(Number);
        return new Date(y, m - 1, d);
    }

    function startOfDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }

    function isBeforeDay(a, b) {
        return a.getTime() < b.getTime();
    }

    function isDateInWeek(date, weekStart) {
        const start = startOfDay(weekStart).getTime();
        const end = start + 6 * 24 * 60 * 60 * 1000;
        const value = startOfDay(date).getTime();
        return value >= start && value <= end;
    }
})();
