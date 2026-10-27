// P9 y P10: atajos de período, validación de fechas antes de consultar y fila que abre el detalle (P8).
// El servidor repite la validación; este script solo evita la recarga cuando las fechas no son válidas.
(() => {
    const FECHA_OBLIGATORIA = "Ingresa la fecha.";
    const RANGO_INVALIDO = "La fecha final debe ser igual o posterior a la inicial.";

    const filtros = document.querySelector("[data-reporte-filtros]");
    if (filtros) {
        const desde = filtros.querySelector("#desde");
        const hasta = filtros.querySelector("#hasta");
        const atajos = filtros.querySelectorAll(".reporte-atajo");

        const mostrarError = (campo, mensaje) => {
            const error = document.getElementById(`${campo.id}Error`);
            campo.classList.toggle("reporte-input-error", Boolean(mensaje));
            campo.setAttribute("aria-invalid", String(Boolean(mensaje)));
            error.hidden = !mensaje;
            if (mensaje) {
                error.querySelector("span").textContent = mensaje;
            }
        };

        // Marca el atajo que coincide con las fechas elegidas (o ninguno).
        const marcarAtajo = () => {
            atajos.forEach((atajo) => {
                const activo = atajo.dataset.desde === desde.value && atajo.dataset.hasta === hasta.value;
                atajo.classList.toggle("active", activo);
                atajo.setAttribute("aria-pressed", String(activo));
            });
        };

        atajos.forEach((atajo) => atajo.addEventListener("click", () => {
            desde.value = atajo.dataset.desde;
            hasta.value = atajo.dataset.hasta;
            mostrarError(desde, null);
            mostrarError(hasta, null);
            marcarAtajo();
        }));

        [desde, hasta].forEach((campo) => campo.addEventListener("change", () => {
            mostrarError(campo, null);
            marcarAtajo();
        }));

        filtros.addEventListener("submit", (evento) => {
            const errorDesde = desde.value ? null : FECHA_OBLIGATORIA;
            let errorHasta = hasta.value ? null : FECHA_OBLIGATORIA;
            // Las fechas ISO (aaaa-mm-dd) se comparan como texto.
            if (!errorDesde && !errorHasta && hasta.value < desde.value) {
                errorHasta = RANGO_INVALIDO;
            }
            mostrarError(desde, errorDesde);
            mostrarError(hasta, errorHasta);
            if (errorDesde || errorHasta) {
                evento.preventDefault();
                (errorDesde ? desde : hasta).focus();
            }
        });
    }

    document.querySelectorAll(".agenda-fila[data-href]").forEach((fila) => {
        fila.addEventListener("click", (evento) => {
            if (!evento.target.closest("a")) {
                window.location.href = fila.dataset.href;
            }
        });
    });
})();
