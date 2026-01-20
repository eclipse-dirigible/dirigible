import { rs } from '@aerokit/sdk/http';
import { getBrandingJs } from '/platform-branding/branding.mjs';

rs.service()
    .resource('')
    .get((_ctx, _request, response) => {
        response.setContentType("text/javascript");
        response.println(getBrandingJs());
    })
    .execute();
