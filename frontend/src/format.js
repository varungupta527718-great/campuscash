let currency='INR';
export function setCurrency(value){currency=value||'INR'}
export function money(value){try{return new Intl.NumberFormat('en-IN',{style:'currency',currency,maximumFractionDigits:2}).format(Number(value)||0)}catch{return `${currency} ${Number(value||0).toFixed(2)}`}}
