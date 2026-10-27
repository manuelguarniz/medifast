package pe.edu.utp.medifast.service.impl;

import org.springframework.stereotype.Service;

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.service.MedicoService;

/** Resuelve el médico autenticado a partir de su n.º de documento. */
@Service
public class MedicoServiceImpl implements MedicoService{
    private final MedicoDao medicoDao;

    public MedicoServiceImpl(MedicoDao medicoDao) {
        this.medicoDao = medicoDao;
    }

    @Override
    public Long obtenerIdPorDocumento(String numeroDocumento) {
        return medicoDao.findByUsuarioNumeroDocumento(numeroDocumento).map(Medico::getId).orElseThrow(() -> new IllegalStateException("No se encontró el médico autenticado"));
    }
}
