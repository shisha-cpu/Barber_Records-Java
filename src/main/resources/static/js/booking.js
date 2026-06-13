(() => {
    const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
        'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
    const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

    const state = {
        step: 0,
        service: null,
        date: null,
        time: null,
        calendarMonth: new Date()
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
    const calendarEl = document.getElementById('calendar');
    const selectedDateInput = document.getElementById('selectedDate');
    const selectedDateLabel = document.getElementById('selectedDateLabel');
    const timeSlots = document.getElementById('timeSlots');
    const noSlotsHint = document.getElementById('noSlotsHint');
    const formError = document.getElementById('formError');
    const formSuccess = document.getElementById('formSuccess');

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

        if (state.step === 2) {
            await loadTimeSlots();
        }
        if (state.step === 4) {
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
        nextBtn.classList.toggle('hidden', step === 5);
        confirmBtn.classList.toggle('hidden', step !== 5);

        if (step === 2) {
            renderCalendar();
        }
        if (step === 3 && state.date) {
            selectedDateLabel.textContent = formatDateLabel(state.date);
        }
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

    function renderCalendar() {
        const year = state.calendarMonth.getFullYear();
        const month = state.calendarMonth.getMonth();
        const today = startOfDay(new Date());

        calendarEl.innerHTML = '';

        const header = document.createElement('div');
        header.className = 'calendar-header';
        header.innerHTML = `
            <button type="button" class="calendar-nav" id="prevMonth">&larr;</button>
            <span class="calendar-title">${MONTHS[month]} ${year}</span>
            <button type="button" class="calendar-nav" id="nextMonth">&rarr;</button>
        `;
        calendarEl.appendChild(header);

        const weekdays = document.createElement('div');
        weekdays.className = 'calendar-weekdays';
        WEEKDAYS.forEach((day) => {
            const cell = document.createElement('div');
            cell.textContent = day;
            weekdays.appendChild(cell);
        });
        calendarEl.appendChild(weekdays);

        const grid = document.createElement('div');
        grid.className = 'calendar-grid';

        const firstDay = new Date(year, month, 1);
        let offset = firstDay.getDay() - 1;
        if (offset < 0) offset = 6;

        for (let i = 0; i < offset; i++) {
            grid.appendChild(document.createElement('div'));
        }

        const daysInMonth = new Date(year, month + 1, 0).getDate();
        for (let day = 1; day <= daysInMonth; day++) {
            const date = new Date(year, month, day);
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'calendar-day';
            btn.textContent = String(day);

            const iso = toIsoDate(date);
            if (date < today) {
                btn.disabled = true;
                btn.classList.add('disabled');
            }
            if (state.date === iso) {
                btn.classList.add('selected');
            }
            btn.addEventListener('click', () => {
                state.date = iso;
                selectedDateInput.value = iso;
                state.time = null;
                renderCalendar();
            });
            grid.appendChild(btn);
        }
        calendarEl.appendChild(grid);

        document.getElementById('prevMonth').addEventListener('click', () => {
            state.calendarMonth = new Date(year, month - 1, 1);
            renderCalendar();
        });
        document.getElementById('nextMonth').addEventListener('click', () => {
            state.calendarMonth = new Date(year, month + 1, 1);
            renderCalendar();
        });
    }

    async function loadTimeSlots() {
        timeSlots.innerHTML = '';
        noSlotsHint.classList.add('hidden');
        state.time = null;

        const params = new URLSearchParams({
            serviceId: state.service.id,
            date: state.date
        });
        const response = await fetch(`/api/times?${params}`);
        const times = await response.json();

        if (!times.length) {
            noSlotsHint.classList.remove('hidden');
            return;
        }

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
            timeSlots.appendChild(btn);
        });
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
        if (step === 2 && !state.date) {
            showError('Выберите дату');
            return false;
        }
        if (step === 3 && !state.time) {
            showError('Выберите время');
            return false;
        }
        if (step === 4) {
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
        return `${Number(value).toLocaleString('ru-RU')} ₽`;
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

    function startOfDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }
})();
