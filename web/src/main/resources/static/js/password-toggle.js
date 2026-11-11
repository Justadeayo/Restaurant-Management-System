(function () {
  var EYE_OPEN = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
  var EYE_OFF = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

  function addToggle(input) {
    if (input.dataset.eyeToggle) {
      return;
    }
    input.dataset.eyeToggle = '1';

    var button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn text-secondary';
    button.innerHTML = EYE_OPEN;
    button.title = 'Show password';
    button.setAttribute('aria-label', 'Show password');
    button.addEventListener('click', function () {
      var showing = input.type === 'text';
      input.type = showing ? 'password' : 'text';
      button.innerHTML = showing ? EYE_OPEN : EYE_OFF;
      button.title = showing ? 'Show password' : 'Hide password';
      button.setAttribute('aria-label', button.title);
    });

    if (input.parentElement && input.parentElement.classList.contains('input-group')) {
      button.classList.add('btn-outline-secondary');
      input.insertAdjacentElement('afterend', button);
    } else {
      var wrap = document.createElement('div');
      wrap.className = 'position-relative';
      input.parentNode.insertBefore(wrap, input);
      wrap.appendChild(input);
      input.classList.add('pe-5');
      button.className = 'btn text-secondary border-0 position-absolute top-50 end-0 translate-middle-y';
      wrap.appendChild(button);
    }
  }

  document.querySelectorAll('input[type="password"]').forEach(addToggle);
})();
